package com.example.fullstackmall.service.cart;

import com.example.fullstackmall.service.FullstackMallApplication;
import com.example.fullstackmall.service.cart.entity.CartItemEntity;
import com.example.fullstackmall.service.cart.service.CartItemDbService;
import com.example.fullstackmall.service.inventory.entity.SkuEntity;
import com.example.fullstackmall.service.inventory.service.SkuDbService;
import com.example.fullstackmall.service.product.entity.ProductEntity;
import com.example.fullstackmall.service.product.service.ProductDbService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FullstackMallApplication.class)
@AutoConfigureMockMvc
@Transactional
class CartControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CartItemDbService cartItemDbService;

    @Resource
    private ProductDbService productDbService;

    @Resource
    private SkuDbService skuDbService;

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":1,\"quantity\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldAddSaleableSkuForCurrentUserAndIgnoreSubmittedUserId() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");
        ProductEntity product = createProduct("ON_SALE");
        SkuEntity sku = createSku(product.getId(), 5);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "skuId": %d,
                                  "quantity": 2,
                                  "userId": 2
                                }
                                """.formatted(sku.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.skuId").value(sku.getId()))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andExpect(jsonPath("$.data.checked").value(true))
                .andExpect(jsonPath("$.data.saleable").value(true))
                .andExpect(jsonPath("$.data.lineAmount").value(176.0));

        CartItemEntity saved = cartItemDbService.getByUserIdAndSkuId(1L, sku.getId());
        assertThat(saved).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(cartItemDbService.getByUserIdAndSkuId(2L, sku.getId())).isNull();
    }

    @Test
    void shouldValidateQuantityRange() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":1,\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":1,\"quantity\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturnNotFoundForMissingSku() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":999999,\"quantity\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SKU_NOT_FOUND"));
    }

    @Test
    void shouldRejectSkuOfDraftProduct() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");
        ProductEntity product = createProduct("DRAFT");
        SkuEntity sku = createSku(product.getId(), 5);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":" + sku.getId() + ",\"quantity\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CART_SKU_NOT_SALEABLE"));
    }

    @Test
    void shouldRejectQuantityExceedingAvailableStock() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");
        ProductEntity product = createProduct("ON_SALE");
        SkuEntity sku = createSku(product.getId(), 2);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":" + sku.getId() + ",\"quantity\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CART_QUANTITY_EXCEEDS_STOCK"));
    }

    @Test
    void shouldRejectDuplicateSkuForSameUser() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":1,\"quantity\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CART_ITEM_ALREADY_EXISTS"));
    }

    @Test
    void shouldQueryOnlyCurrentUserAndAssembleProductSkuAndAmount() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");

        mockMvc.perform(get("/api/cart/items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(1))
                .andExpect(jsonPath("$.data.items[0].skuId").value(1))
                .andExpect(jsonPath("$.data.items[0].productTitle").value("九成新 iPhone 15"))
                .andExpect(jsonPath("$.data.items[0].skuCode").value("IPHONE15-BLACK-128G"))
                .andExpect(jsonPath("$.data.items[0].specText").value("黑色 / 128G"))
                .andExpect(jsonPath("$.data.items[0].currentUnitPrice").value(4599.0))
                .andExpect(jsonPath("$.data.items[0].availableStock").value(10))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].saleable").value(true))
                .andExpect(jsonPath("$.data.items[0].lineAmount").value(9198.0))
                .andExpect(jsonPath("$.data.totalQuantity").value(2))
                .andExpect(jsonPath("$.data.estimatedTotal").value(9198.0));
    }

    @Test
    void shouldExcludeUncheckedRowsFromEstimatedTotalAndIsolateOtherUser() throws Exception {
        String token = loginAndGetToken("xiaohong", "Mall123456");

        mockMvc.perform(get("/api/cart/items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(2))
                .andExpect(jsonPath("$.data.items[0].checked").value(false))
                .andExpect(jsonPath("$.data.items[0].productTitle").value("深入理解计算机系统"))
                .andExpect(jsonPath("$.data.totalQuantity").value(1))
                .andExpect(jsonPath("$.data.estimatedTotal").value(0.0));
    }

    @Test
    void shouldKeepRowButMarkItUnsaleableAfterProductOffShelf() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");
        ProductEntity product = productDbService.getById(1L);
        product.setStatusCode("OFF_SHELF");
        product.setUpdatedAt(LocalDateTime.now());
        productDbService.updateById(product);

        mockMvc.perform(get("/api/cart/items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].saleable").value(false))
                .andExpect(jsonPath("$.data.items[0].productTitle").value("九成新 iPhone 15"))
                .andExpect(jsonPath("$.data.estimatedTotal").value(0.0));
    }

    @Test
    void shouldKeepRowButMarkItUnsaleableWhenStockBecomesInsufficient() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");
        SkuEntity sku = skuDbService.getById(1L);
        sku.setAvailableStock(1);
        sku.setUpdatedAt(LocalDateTime.now());
        skuDbService.updateById(sku);

        mockMvc.perform(get("/api/cart/items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].availableStock").value(1))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].saleable").value(false))
                .andExpect(jsonPath("$.data.estimatedTotal").value(0.0));
    }

    @Test
    void shouldKeepRowWhenSkuWasDeleted() throws Exception {
        String token = loginAndGetToken("xiaoming", "Mall123456");
        skuDbService.removeById(1L);

        mockMvc.perform(get("/api/cart/items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].skuId").value(1))
                .andExpect(jsonPath("$.data.items[0].productId").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].currentUnitPrice").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].saleable").value(false))
                .andExpect(jsonPath("$.data.estimatedTotal").value(0.0));
    }

    @Test
    void shouldLetAdminSeeOnlyAdminsOwnEmptyCart() throws Exception {
        String token = loginAndGetToken("admin", "Admin123456");

        mockMvc.perform(get("/api/cart/items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.totalQuantity").value(0))
                .andExpect(jsonPath("$.data.estimatedTotal").value(0.0));
    }

    private ProductEntity createProduct(String statusCode) {
        LocalDateTime now = LocalDateTime.now();
        ProductEntity product = new ProductEntity();
        product.setCategoryId(1L);
        product.setTitle("购物车测试商品 " + UUID.randomUUID());
        product.setDescription("购物车测试");
        product.setStatusCode(statusCode);
        product.setCreatedBy(5L);
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        productDbService.save(product);
        return product;
    }

    private SkuEntity createSku(Long productId, int stock) {
        LocalDateTime now = LocalDateTime.now();
        SkuEntity sku = new SkuEntity();
        sku.setProductId(productId);
        sku.setSkuCode("CART-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        sku.setSpecText("购物车测试规格");
        sku.setSalePrice(new BigDecimal("88.00"));
        sku.setAvailableStock(stock);
        sku.setLockedStock(0);
        sku.setVersion(0);
        sku.setCreatedAt(now);
        sku.setUpdatedAt(now);
        skuDbService.save(sku);
        return sku;
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password)
                        )))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("accessToken").asText();
    }
}
