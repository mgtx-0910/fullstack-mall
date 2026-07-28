package com.example.fullstackmall.service.payment;

import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.payment.MockPaymentCallbackRequest;
import com.example.fullstackmall.contract.payment.PaymentCallbackResult;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.order.OrderCloseTransactionService;
import com.example.fullstackmall.service.order.OrderCreateTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Sql(
        scripts = {
                "classpath:db/test-schema.sql",
                "classpath:db/test-data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = {
                "classpath:db/test-schema.sql",
                "classpath:db/test-data.sql"
        },
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class PaymentTransactionRollbackTest {

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

    @Test
    void callbackRollsBackOrderAndPaymentWhenStockConfirmationFails() {
        long orderId = createOrderAndPayment("payment-rollback-callback-001");
        String paymentNo = text("SELECT payment_no FROM mall_payment WHERE order_id = " + orderId);
        jdbcTemplate.update("UPDATE mall_product_sku SET locked_stock = 0 WHERE id = 1");

        MockPaymentCallbackRequest request = MockPaymentCallbackRequest.builder()
                .paymentNo(paymentNo)
                .callbackId("callback-rollback-001")
                .providerTransactionNo("provider-rollback-001")
                .result(PaymentCallbackResult.SUCCESS)
                .build();

        assertThatThrownBy(() -> paymentCallbackTransactionService.handleSuccess(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ApiCode.PAYMENT_STOCK_CONFIRM_FAILED));

        assertThat(text("SELECT status_code FROM mall_order WHERE id = " + orderId))
                .isEqualTo("PENDING_PAYMENT");
        assertThat(text("SELECT status_code FROM mall_payment WHERE order_id = " + orderId))
                .isEqualTo("PENDING");
        assertThat(nullableText("SELECT provider_transaction_no FROM mall_payment WHERE order_id = " + orderId))
                .isNull();
        assertThat(nullableText("SELECT callback_id FROM mall_payment WHERE order_id = " + orderId))
                .isNull();
    }

    @Test
    void closeRollsBackOrderAndPaymentWhenStockReleaseFails() {
        long orderId = createOrderAndPayment("payment-rollback-close-001");
        jdbcTemplate.update("UPDATE mall_order SET expires_at = ? WHERE id = ?",
                LocalDateTime.now().minusMinutes(1), orderId);
        jdbcTemplate.update("UPDATE mall_product_sku SET locked_stock = 0 WHERE id = 1");

        assertThatThrownBy(() -> orderCloseTransactionService.closeExpiredOrder(orderId, LocalDateTime.now()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ApiCode.ORDER_CLOSE_STOCK_RELEASE_FAILED));

        assertThat(text("SELECT status_code FROM mall_order WHERE id = " + orderId))
                .isEqualTo("PENDING_PAYMENT");
        assertThat(text("SELECT status_code FROM mall_payment WHERE order_id = " + orderId))
                .isEqualTo("PENDING");
        assertThat(nullableText("SELECT closed_at FROM mall_order WHERE id = " + orderId)).isNull();
        assertThat(nullableText("SELECT closed_at FROM mall_payment WHERE order_id = " + orderId)).isNull();
    }

    private long createOrderAndPayment(String idempotencyKey) {
        long orderId = orderCreateTransactionService.createOrder(1L, idempotencyKey);
        paymentCreateTransactionService.createPayment(orderId, 1L);
        return orderId;
    }

    private String text(String sql) {
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    private String nullableText(String sql) {
        return jdbcTemplate.queryForObject(sql, String.class);
    }
}
