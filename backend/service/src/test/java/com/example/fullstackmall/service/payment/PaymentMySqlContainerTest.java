package com.example.fullstackmall.service.payment;

import com.example.fullstackmall.contract.payment.MockPaymentCallbackRequest;
import com.example.fullstackmall.contract.payment.PaymentCallbackResult;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.order.OrderCloseTransactionService;
import com.example.fullstackmall.service.order.OrderCreateTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@Testcontainers
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_CONTAINER_TESTS", matches = "true")
@Sql(
        scripts = "classpath:db/mysql-container-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
        config = @SqlConfig(separator = ";")
)
class PaymentMySqlContainerTest {

    static final MySQLContainer<?> MYSQL = startMySql();

    private static MySQLContainer<?> startMySql() {
        MySQLContainer<?> container = new MySQLContainer<>("mysql:8.4")
                .withDatabaseName("fullstack_mall")
                .withUsername("mall")
                .withPassword("mall");
        container.start();
        return container;
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private OrderCreateTransactionService orderCreateTransactionService;

    @Autowired
    private PaymentCreateTransactionService paymentCreateTransactionService;

    @Autowired
    private PaymentCallbackTransactionService paymentCallbackTransactionService;

    @Autowired
    private OrderCloseTransactionService orderCloseTransactionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM mall_payment");
        jdbcTemplate.update("DELETE FROM mall_order_item");
        jdbcTemplate.update("DELETE FROM mall_order");
        jdbcTemplate.update("DELETE FROM mall_cart_item");
        jdbcTemplate.update("DELETE FROM mall_product_sku");
        jdbcTemplate.update("DELETE FROM mall_product");

        jdbcTemplate.update("""
                INSERT INTO mall_product
                    (id, category_id, title, description, status_code, created_by, created_at, updated_at)
                VALUES (1, 1, '九成新 iPhone 15', '支付并发测试商品', 'ON_SALE', 5, NOW(3), NOW(3))
                """);
        jdbcTemplate.update("""
                INSERT INTO mall_product_sku
                    (id, product_id, sku_code, spec_text, sale_price, available_stock, locked_stock, version, created_at, updated_at)
                VALUES (1, 1, 'IPHONE15-BLACK-128G', '黑色 / 128G', 4599.00, 10, 0, 0, NOW(3), NOW(3))
                """);
        jdbcTemplate.update("""
                INSERT INTO mall_cart_item
                    (user_id, sku_id, quantity, checked, created_at, updated_at)
                VALUES (1, 1, 2, 1, NOW(3), NOW(3))
                """);
    }

    @Test
    void concurrentCreatePaymentReturnsOnePayment() throws Exception {
        Long orderId = orderCreateTransactionService.createOrder(1L, "mysql-payment-create-order-001");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Long> first = executor.submit(() -> createPaymentConcurrently(orderId, ready, start));
            Future<Long> second = executor.submit(() -> createPaymentConcurrently(orderId, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(second.get(10, TimeUnit.SECONDS))
                    .isEqualTo(first.get(10, TimeUnit.SECONDS));
            assertThat(count("mall_payment")).isEqualTo(1);
            assertThat(text("SELECT status_code FROM mall_payment WHERE order_id = " + orderId))
                    .isEqualTo("PENDING");
            assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(8);
            assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(2);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentSuccessCallbacksConfirmStockOnlyOnce() throws Exception {
        Long orderId = createOrderAndPayment("mysql-payment-callback-order-001");
        String paymentNo = text("SELECT payment_no FROM mall_payment WHERE order_id = " + orderId);
        MockPaymentCallbackRequest request = callbackRequest(
                paymentNo,
                "mysql-callback-success-001",
                "mysql-provider-success-001"
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<String> first = executor.submit(() -> callbackConcurrently(request, ready, start));
            Future<String> second = executor.submit(() -> callbackConcurrently(request, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            )).containsOnly("CALLBACK_SUCCESS");
            assertThat(text("SELECT status_code FROM mall_order WHERE id = " + orderId)).isEqualTo("PAID");
            assertThat(text("SELECT status_code FROM mall_payment WHERE order_id = " + orderId))
                    .isEqualTo("SUCCESS");
            assertThat(text("SELECT callback_id FROM mall_payment WHERE order_id = " + orderId))
                    .isEqualTo("mysql-callback-success-001");
            assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(8);
            assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void callbackAndTimeoutCloseHaveOneConsistentWinner() throws Exception {
        Long orderId = createOrderAndPayment("mysql-payment-race-order-001");
        jdbcTemplate.update(
                "UPDATE mall_order SET expires_at = ? WHERE id = ?",
                LocalDateTime.now().minusMinutes(1),
                orderId
        );
        String paymentNo = text("SELECT payment_no FROM mall_payment WHERE order_id = " + orderId);
        MockPaymentCallbackRequest request = callbackRequest(
                paymentNo,
                "mysql-callback-race-001",
                "mysql-provider-race-001"
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<String> callback = executor.submit(() -> callbackConcurrently(request, ready, start));
            Future<String> close = executor.submit(() -> closeConcurrently(orderId, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<String> results = List.of(
                    callback.get(10, TimeUnit.SECONDS),
                    close.get(10, TimeUnit.SECONDS)
            );
            String orderStatus = text("SELECT status_code FROM mall_order WHERE id = " + orderId);
            String paymentStatus = text("SELECT status_code FROM mall_payment WHERE order_id = " + orderId);
            int availableStock = integer("SELECT available_stock FROM mall_product_sku WHERE id = 1");
            int lockedStock = integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1");

            if ("PAID".equals(orderStatus)) {
                assertThat(paymentStatus).isEqualTo("SUCCESS");
                assertThat(availableStock).isEqualTo(8);
                assertThat(lockedStock).isZero();
                assertThat(results).contains("CALLBACK_SUCCESS", "CLOSE_FALSE");
            } else {
                assertThat(orderStatus).isEqualTo("CLOSED");
                assertThat(paymentStatus).isEqualTo("CLOSED");
                assertThat(availableStock).isEqualTo(10);
                assertThat(lockedStock).isZero();
                assertThat(results).contains("CLOSE_TRUE", "CALLBACK_PAYMENT_STATUS_CONFLICT");
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private Long createOrderAndPayment(String idempotencyKey) {
        Long orderId = orderCreateTransactionService.createOrder(1L, idempotencyKey);
        paymentCreateTransactionService.createPayment(orderId, 1L);
        return orderId;
    }

    private Long createPaymentConcurrently(
            Long orderId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        return paymentCreateTransactionService.createPayment(orderId, 1L);
    }

    private String callbackConcurrently(
            MockPaymentCallbackRequest request,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            paymentCallbackTransactionService.handleSuccess(request);
            return "CALLBACK_SUCCESS";
        } catch (BusinessException exception) {
            return "CALLBACK_" + exception.getCode().name();
        }
    }

    private String closeConcurrently(
            Long orderId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            boolean closed = orderCloseTransactionService.closeExpiredOrder(orderId, LocalDateTime.now());
            return closed ? "CLOSE_TRUE" : "CLOSE_FALSE";
        } catch (BusinessException exception) {
            return "CLOSE_" + exception.getCode().name();
        }
    }

    private MockPaymentCallbackRequest callbackRequest(
            String paymentNo,
            String callbackId,
            String providerTransactionNo
    ) {
        return MockPaymentCallbackRequest.builder()
                .paymentNo(paymentNo)
                .callbackId(callbackId)
                .providerTransactionNo(providerTransactionNo)
                .result(PaymentCallbackResult.SUCCESS)
                .build();
    }

    private int count(String table) {
        Integer value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return value == null ? 0 : value;
    }

    private int integer(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private String text(String sql) {
        return jdbcTemplate.queryForObject(sql, String.class);
    }
}
