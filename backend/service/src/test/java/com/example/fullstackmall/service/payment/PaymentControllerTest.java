package com.example.fullstackmall.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.fullstackmall.service.order.OrderCloseBatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
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
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderCloseBatchService orderCloseBatchService;

    @Test
    void anonymousUserCannotCreateOrQueryPayment() throws Exception {
        mockMvc.perform(post("/api/orders/1/payments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/orders/1/payment"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsQueriesAndRetriesSamePayment() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");
        long orderId = createOrder(token, "payment-create-order-001");

        JsonNode first = createPayment(token, orderId);
        JsonNode second = createPayment(token, orderId);

        assertThat(second.path("data").path("id").asLong())
                .isEqualTo(first.path("data").path("id").asLong());
        assertThat(count("mall_payment")).isEqualTo(1);

        mockMvc.perform(get("/api/orders/{orderId}/payment", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentNo").value(first.path("data").path("paymentNo").asText()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.amount").value(9198.00));
    }

    @Test
    void otherUserCannotCreateOrQueryPayment() throws Exception {
        String ownerToken = loginAndGetToken("xiaoming", "Mall123456");
        long orderId = createOrder(ownerToken, "payment-owner-order-001");
        String otherToken = loginAndGetToken("xiaohong", "Mall123456");

        mockMvc.perform(post("/api/orders/{orderId}/payments", orderId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        createPayment(ownerToken, orderId);
        mockMvc.perform(get("/api/orders/{orderId}/payment", orderId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
    }

    @Test
    void callbackRequiresSecretAndIsIdempotent() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");
        long orderId = createOrder(token, "payment-callback-order-001");
        String paymentNo = createPayment(token, orderId).path("data").path("paymentNo").asText();
        String callbackBody = callbackBody(paymentNo, "callback-h2-001", "provider-h2-001");

        mockMvc.perform(post("/api/mock-payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Mock-Payment-Secret", "wrong-secret")
                        .content(callbackBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PAYMENT_CALLBACK_SECRET_INVALID"));

        performCallback(callbackBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        performCallback(callbackBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        assertThat(text("SELECT status_code FROM mall_order WHERE id = " + orderId)).isEqualTo("PAID");
        assertThat(text("SELECT status_code FROM mall_payment WHERE order_id = " + orderId)).isEqualTo("SUCCESS");
        assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(8);
        assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isZero();
    }

    @Test
    void cancelledOrderRejectsLateCallbackAndKeepsReleasedStock() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");
        long orderId = createOrder(token, "payment-cancel-order-001");
        String paymentNo = createPayment(token, orderId).path("data").path("paymentNo").asText();

        mockMvc.perform(patch("/api/orders/{orderId}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        performCallback(callbackBody(paymentNo, "callback-late-001", "provider-late-001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_STATUS_CONFLICT"));

        assertThat(text("SELECT status_code FROM mall_payment WHERE order_id = " + orderId)).isEqualTo("CLOSED");
        assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(10);
        assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isZero();
    }

    @Test
    void closesExpiredOrderAndReleasesStock() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");
        long orderId = createOrder(token, "payment-close-order-001");
        createPayment(token, orderId);
        jdbcTemplate.update("UPDATE mall_order SET expires_at = DATEADD('MINUTE', -1, CURRENT_TIMESTAMP) WHERE id = ?", orderId);

        assertThat(orderCloseBatchService.closeExpiredOrders()).isEqualTo(1);
        assertThat(text("SELECT status_code FROM mall_order WHERE id = " + orderId)).isEqualTo("CLOSED");
        assertThat(text("SELECT status_code FROM mall_payment WHERE order_id = " + orderId)).isEqualTo("CLOSED");
        assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(10);
        assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isZero();
    }

    @Test
    void paidOrderIsNotClosedByBatch() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");
        long orderId = createOrder(token, "payment-paid-order-001");
        String paymentNo = createPayment(token, orderId).path("data").path("paymentNo").asText();
        performCallback(callbackBody(paymentNo, "callback-paid-001", "provider-paid-001"))
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE mall_order SET expires_at = DATEADD('MINUTE', -1, CURRENT_TIMESTAMP) WHERE id = ?", orderId);

        assertThat(orderCloseBatchService.closeExpiredOrders()).isZero();
        assertThat(text("SELECT status_code FROM mall_order WHERE id = " + orderId)).isEqualTo("PAID");
        assertThat(text("SELECT status_code FROM mall_payment WHERE order_id = " + orderId)).isEqualTo("SUCCESS");
        assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(8);
        assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isZero();
    }

    private long createOrder(String token, String idempotencyKey) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private JsonNode createPayment(String token, long orderId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders/{orderId}/payments", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private org.springframework.test.web.servlet.ResultActions performCallback(String body) throws Exception {
        return mockMvc.perform(post("/api/mock-payments/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Mock-Payment-Secret", "your-local-mock-payment-secret")
                .content(body));
    }

    private String callbackBody(String paymentNo, String callbackId, String providerTransactionNo) {
        return """
                {
                  "paymentNo": "%s",
                  "callbackId": "%s",
                  "providerTransactionNo": "%s",
                  "result": "SUCCESS"
                }
                """.formatted(paymentNo, callbackId, providerTransactionNo);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
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
