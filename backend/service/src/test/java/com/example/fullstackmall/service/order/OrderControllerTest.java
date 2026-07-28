package com.example.fullstackmall.service.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

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
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void anonymousUserCannotCreateQueryOrCancelOrder() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "anonymous-001"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/orders/1/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsOrderFromCheckedCartAndLocksStock() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "order-create-001"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.totalAmount").value(9198.00))
                .andExpect(jsonPath("$.data.items[0].productTitle").value("九成新 iPhone 15"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(4599.00))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].lineAmount").value(9198.00));

        assertThat(count("mall_order")).isEqualTo(1);
        assertThat(count("mall_order_item")).isEqualTo(1);
        assertThat(count("mall_cart_item WHERE user_id = 1 AND checked = 1")).isZero();
        assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(8);
        assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(2);
    }

    @Test
    void sameIdempotencyKeyReturnsOriginalOrderWithoutLockingAgain() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");
        String firstBody = createOrder(token, "order-retry-001");
        String secondBody = createOrder(token, "order-retry-001");

        Long firstId = objectMapper.readTree(firstBody).path("data").path("id").asLong();
        Long secondId = objectMapper.readTree(secondBody).path("data").path("id").asLong();

        assertThat(secondId).isEqualTo(firstId);
        assertThat(count("mall_order")).isEqualTo(1);
        assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(8);
        assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(2);
    }

    @Test
    void orderQueryUsesSnapshotInsteadOfCurrentProductData() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");
        JsonNode created = objectMapper.readTree(createOrder(token, "order-snapshot-001"));
        long orderId = created.path("data").path("id").asLong();

        jdbcTemplate.update("UPDATE mall_product SET title = ? WHERE id = 1", "商品后来改名了");
        jdbcTemplate.update("UPDATE mall_product_sku SET sale_price = ? WHERE id = 1", new BigDecimal("3999.00"));

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].productTitle").value("九成新 iPhone 15"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(4599.00));
    }

    @Test
    void anotherUserCannotReadOrder() throws Exception {
        String ownerToken = loginAndGetToken("xiaoming", "Mall123456");
        long orderId = objectMapper.readTree(createOrder(ownerToken, "order-owner-001"))
                .path("data").path("id").asLong();
        String otherToken = loginAndGetToken("xiaohong", "Mall123456");

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void cancellingPendingOrderReleasesStockOnlyOnce() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");
        long orderId = objectMapper.readTree(createOrder(token, "order-cancel-001"))
                .path("data").path("id").asLong();

        mockMvc.perform(patch("/api/orders/{orderId}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledAt").isNotEmpty());

        assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(10);
        assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isZero();

        mockMvc.perform(patch("/api/orders/{orderId}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_STATUS_CONFLICT"));

        assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(10);
        assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isZero();
    }

    @Test
    void rejectsInvalidIdempotencyKey() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ORDER_IDEMPOTENCY_KEY_INVALID"));
    }

    @Test
    void rejectsCartWithoutCheckedItems() throws Exception {
        String token = loginAndGetToken("xiaohong", "Mall123456");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "order-empty-001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_CART_EMPTY"));
    }

    @Test
    void rejectsOffShelfProductWithoutChangingData() throws Exception {
        jdbcTemplate.update("UPDATE mall_product SET status_code = 'OFF_SHELF' WHERE id = 1");
        String token = loginAndGetToken("xiaoming", "Mall123456");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "order-offsale-001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_ITEM_NOT_SALEABLE"));

        assertThat(count("mall_order")).isZero();
        assertThat(count("mall_cart_item WHERE user_id = 1")).isEqualTo(1);
        assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(10);
    }

    @Test
    void rejectsInsufficientStockWithoutChangingData() throws Exception {
        jdbcTemplate.update("UPDATE mall_product_sku SET available_stock = 1 WHERE id = 1");
        String token = loginAndGetToken("xiaoming", "Mall123456");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "order-stock-001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_INSUFFICIENT_STOCK"));

        assertThat(count("mall_order")).isZero();
        assertThat(count("mall_cart_item WHERE user_id = 1")).isEqualTo(1);
        assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(1);
        assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isZero();
    }

    private String createOrder(String token, String idempotencyKey) throws Exception {
        return mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
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
                .path("data")
                .path("accessToken")
                .asText();
    }

    private int count(String tableAndCondition) {
        Integer value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableAndCondition, Integer.class);
        return value == null ? 0 : value;
    }

    private int integer(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
