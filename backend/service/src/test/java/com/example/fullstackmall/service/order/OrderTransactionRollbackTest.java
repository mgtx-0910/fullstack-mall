package com.example.fullstackmall.service.order;

import com.example.fullstackmall.service.order.service.OrderItemDbService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class OrderTransactionRollbackTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private OrderItemDbService orderItemDbService;

    @Test
    void rollsBackOrderAndStockWhenSavingOrderItemsFails() throws Exception {
        doThrow(new RuntimeException("模拟订单项保存失败"))
                .when(orderItemDbService)
                .saveBatch(anyCollection());
        String token = loginAndGetToken();

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "order-rollback-001"))
                .andExpect(status().isInternalServerError());

        assertThat(count("mall_order")).isZero();
        assertThat(count("mall_order_item")).isZero();
        assertThat(count("mall_cart_item WHERE user_id = 1 AND checked = 1")).isEqualTo(1);
        assertThat(integer("SELECT available_stock FROM mall_product_sku WHERE id = 1")).isEqualTo(10);
        assertThat(integer("SELECT locked_stock FROM mall_product_sku WHERE id = 1")).isZero();
    }

    private String loginAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "xiaoming",
                                  "password": "Mall123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
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
