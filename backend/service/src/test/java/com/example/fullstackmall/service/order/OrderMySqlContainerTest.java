package com.example.fullstackmall.service.order;

import com.example.fullstackmall.contract.auth.UserRole;
import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.order.IOrderFacade;
import com.example.fullstackmall.contract.order.OrderResponse;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.order.service.OrderItemDbService;
import com.example.fullstackmall.service.security.CurrentUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@Testcontainers
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_CONTAINER_TESTS", matches = "true")
@Sql(
        scripts = "classpath:db/mysql-container-schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
        config = @SqlConfig(separator = ";")
)
class OrderMySqlContainerTest {

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IOrderFacade orderFacade;

    @MockitoSpyBean
    private OrderItemDbService orderItemDbService;

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
                VALUES (1, 1, '九成新 iPhone 15', '测试商品', 'ON_SALE', 5, NOW(3), NOW(3))
                """);
        jdbcTemplate.update("""
                INSERT INTO mall_product_sku
                    (id, product_id, sku_code, spec_text, sale_price, available_stock, locked_stock, version, created_at, updated_at)
                VALUES (1, 1, 'IPHONE15-BLACK-128G', '黑色 / 128G', 4599.00, 10, 0, 0, NOW(3), NOW(3))
                """);
        insertCartItem(1L, 2);
    }

    @Test
    void createsOrderAtomicallyOnRealMySql() {
        Long orderId = orderCreateTransactionService.createOrder(1L, "mysql-create-001");

        assertThat(orderId).isPositive();
        assertThat(count("mall_order")).isEqualTo(1);
        assertThat(count("mall_order_item")).isEqualTo(1);
        assertThat(count("mall_cart_item")).isZero();
        assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(8);
        assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(2);
    }

    @Test
    void concurrentOrdersNeverOversell() throws Exception {
        jdbcTemplate.update("UPDATE mall_product_sku SET available_stock = 1 WHERE id = 1");
        jdbcTemplate.update("UPDATE mall_cart_item SET quantity = 1 WHERE user_id = 1");
        insertCartItem(2L, 1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<String> first = executor.submit(() -> createConcurrently(1L, "mysql-race-user-1", ready, start));
            Future<String> second = executor.submit(() -> createConcurrently(2L, "mysql-race-user-2", ready, start));
            ready.await();
            start.countDown();

            List<String> results = List.of(first.get(), second.get());
            assertThat(results).containsExactlyInAnyOrder("SUCCESS", ApiCode.ORDER_INSUFFICIENT_STOCK.name());
            assertThat(count("mall_order")).isEqualTo(1);
            assertThat(count("mall_order_item")).isEqualTo(1);
            assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isZero();
            assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentSameIdempotencyKeyReturnsOneOrder() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Long> first = executor.submit(
                    () -> createThroughFacadeConcurrently("mysql-same-key-001", ready, start)
            );
            Future<Long> second = executor.submit(
                    () -> createThroughFacadeConcurrently("mysql-same-key-001", ready, start)
            );
            ready.await();
            start.countDown();

            assertThat(second.get()).isEqualTo(first.get());
            assertThat(count("mall_order")).isEqualTo(1);
            assertThat(count("mall_order_item")).isEqualTo(1);
            assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(8);
            assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(2);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void realMySqlTransactionRollsBackAfterStockWasLocked() {
        doThrow(new RuntimeException("模拟订单项保存失败"))
                .when(orderItemDbService)
                .saveBatch(anyCollection());

        assertThatThrownBy(() -> orderCreateTransactionService.createOrder(1L, "mysql-rollback-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("模拟订单项保存失败");

        assertThat(count("mall_order")).isZero();
        assertThat(count("mall_order_item")).isZero();
        assertThat(count("mall_cart_item")).isEqualTo(1);
        assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(10);
        assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isZero();
    }

    private String createConcurrently(
            Long userId,
            String idempotencyKey,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            orderCreateTransactionService.createOrder(userId, idempotencyKey);
            return "SUCCESS";
        } catch (BusinessException exception) {
            return exception.getCode().name();
        }
    }

    private Long createThroughFacadeConcurrently(
            String idempotencyKey,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        CurrentUserPrincipal principal = CurrentUserPrincipal.builder()
                .id(1L)
                .username("xiaoming")
                .nickname("小明")
                .role(UserRole.USER)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
        ready.countDown();
        start.await();
        try {
            OrderResponse response = orderFacade.createOrder(idempotencyKey);
            return response.getId();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void insertCartItem(Long userId, int quantity) {
        jdbcTemplate.update("""
                INSERT INTO mall_cart_item
                    (user_id, sku_id, quantity, checked, created_at, updated_at)
                VALUES (?, 1, ?, 1, NOW(3), NOW(3))
                """, userId, quantity);
    }

    private int count(String table) {
        Integer value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return value == null ? 0 : value;
    }

    private int integer(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
