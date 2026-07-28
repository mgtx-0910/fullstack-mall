package com.example.fullstackmall.service.inventory;

import com.example.fullstackmall.service.FullstackMallApplication;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FullstackMallApplication.class)
@AutoConfigureMockMvc
@Transactional
class SkuControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ProductDbService productDbService;

    @Resource
    private SkuDbService skuDbService;

    @Test
    void shouldAllowAdminToCreateAndListSku() throws Exception {
        String token = loginAndGetToken("admin", "Admin123456");
        ProductEntity product = createProduct("DRAFT");
        String skuCode = uniqueCode("CREATE");

        mockMvc.perform(post("/api/admin/products/{productId}/skus", product.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "skuCode":"  %s  ",
                                  "specText":"  黑色 / 256G  ",
                                  "salePrice":4599.00,
                                  "initialStock":10
                                }
                                """.formatted(skuCode)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.skuCode").value(skuCode))
                .andExpect(jsonPath("$.data.specText").value("黑色 / 256G"))
                .andExpect(jsonPath("$.data.salePrice").value(4599.00))
                .andExpect(jsonPath("$.data.availableStock").value(10))
                .andExpect(jsonPath("$.data.lockedStock").value(0))
                .andExpect(jsonPath("$.data.version").value(0));

        SkuEntity saved = skuDbService.lambdaQuery().eq(SkuEntity::getSkuCode, skuCode).one();
        assertThat(saved).isNotNull();
        assertThat(saved.getSalePrice()).isEqualByComparingTo("4599.00");
        assertThat(saved.getLockedStock()).isZero();
        assertThat(saved.getVersion()).isZero();

        mockMvc.perform(get("/api/admin/products/{productId}/skus", product.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].skuCode").value(skuCode));
    }

    @Test
    void shouldValidatePriceStockAndDelta() throws Exception {
        String token = loginAndGetToken("admin", "Admin123456");
        ProductEntity product = createProduct("DRAFT");

        mockMvc.perform(post("/api/admin/products/{productId}/skus", product.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(uniqueCode("SCALE"), "12.345", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/admin/products/{productId}/skus", product.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(uniqueCode("ZERO"), "0", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/admin/products/{productId}/skus", product.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(uniqueCode("NEG"), "10.00", -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(patch("/api/admin/skus/1/stock")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\":0,\"expectedVersion\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectDuplicateCodeAndOnSaleProduct() throws Exception {
        String token = loginAndGetToken("admin", "Admin123456");
        ProductEntity draft = createProduct("DRAFT");

        mockMvc.perform(post("/api/admin/products/{productId}/skus", draft.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("IPHONE15-BLACK-128G", "100.00", 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SKU_CODE_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/admin/products/1/skus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(uniqueCode("ONSALE"), "100.00", 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_SKU_NOT_EDITABLE"));
    }

    @Test
    void shouldProtectAdminSkuEndpoints() throws Exception {
        ProductEntity product = createProduct("DRAFT");
        String body = createBody(uniqueCode("AUTH"), "100.00", 1);

        mockMvc.perform(post("/api/admin/products/{productId}/skus", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        String userToken = loginAndGetToken("xiaoming", "Mall123456");
        mockMvc.perform(post("/api/admin/products/{productId}/skus", product.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldExposeSkuOnlyForPublishedProduct() throws Exception {
        mockMvc.perform(get("/api/products/1/skus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].skuCode").value("IPHONE15-BLACK-128G"));

        ProductEntity draft = createProduct("DRAFT");
        createSku(draft.getId(), uniqueCode("HIDDEN"), new BigDecimal("99.00"), 1, 0);
        mockMvc.perform(get("/api/products/{productId}/skus", draft.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldUpdatePriceWithVersionAndRejectStaleVersion() throws Exception {
        String token = loginAndGetToken("admin", "Admin123456");
        ProductEntity product = createProduct("DRAFT");
        SkuEntity sku = createSku(product.getId(), uniqueCode("PRICE"), new BigDecimal("100.00"), 3, 0);

        mockMvc.perform(patch("/api/admin/skus/{skuId}/price", sku.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salePrice\":88.50,\"expectedVersion\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.salePrice").value(88.50))
                .andExpect(jsonPath("$.data.version").value(1));

        mockMvc.perform(patch("/api/admin/skus/{skuId}/price", sku.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salePrice\":77.00,\"expectedVersion\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVENTORY_VERSION_CONFLICT"));

        SkuEntity current = skuDbService.getById(sku.getId());
        assertThat(current.getSalePrice()).isEqualByComparingTo("88.50");
        assertThat(current.getVersion()).isEqualTo(1);
    }

    @Test
    void shouldAdjustStockAndRejectInsufficientStock() throws Exception {
        String token = loginAndGetToken("admin", "Admin123456");
        ProductEntity product = createProduct("DRAFT");
        SkuEntity sku = createSku(product.getId(), uniqueCode("STOCK"), new BigDecimal("20.00"), 2, 0);

        mockMvc.perform(patch("/api/admin/skus/{skuId}/stock", sku.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\":3,\"expectedVersion\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableStock").value(5))
                .andExpect(jsonPath("$.data.version").value(1));

        mockMvc.perform(patch("/api/admin/skus/{skuId}/stock", sku.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\":-6,\"expectedVersion\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        SkuEntity current = skuDbService.getById(sku.getId());
        assertThat(current.getAvailableStock()).isEqualTo(5);
        assertThat(current.getVersion()).isEqualTo(1);
    }

    @Test
    void shouldRejectPublishingWithoutSaleableSku() throws Exception {
        String token = loginAndGetToken("admin", "Admin123456");
        ProductEntity withoutSku = createProduct("DRAFT");

        mockMvc.perform(patch("/api/admin/products/{id}/status", withoutSku.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"ON_SALE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_READY_FOR_SALE"));

        ProductEntity zeroStock = createProduct("DRAFT");
        createSku(zeroStock.getId(), uniqueCode("EMPTY"), new BigDecimal("10.00"), 0, 0);
        mockMvc.perform(patch("/api/admin/products/{id}/status", zeroStock.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"ON_SALE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_READY_FOR_SALE"));
    }

    private ProductEntity createProduct(String status) {
        LocalDateTime now = LocalDateTime.now();
        ProductEntity product = new ProductEntity();
        product.setCategoryId(1L);
        product.setTitle("SKU 测试商品 " + UUID.randomUUID());
        product.setDescription("SKU 测试");
        product.setStatusCode(status);
        product.setCreatedBy(5L);
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        productDbService.save(product);
        return product;
    }

    private SkuEntity createSku(Long productId, String code, BigDecimal price, int stock, int version) {
        LocalDateTime now = LocalDateTime.now();
        SkuEntity sku = new SkuEntity();
        sku.setProductId(productId);
        sku.setSkuCode(code);
        sku.setSpecText("测试规格");
        sku.setSalePrice(price);
        sku.setAvailableStock(stock);
        sku.setLockedStock(0);
        sku.setVersion(version);
        sku.setCreatedAt(now);
        sku.setUpdatedAt(now);
        skuDbService.save(sku);
        return sku;
    }

    private String createBody(String code, String price, int stock) {
        return """
                {"skuCode":"%s","specText":"测试规格","salePrice":%s,"initialStock":%d}
                """.formatted(code, price, stock);
    }

    private String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
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
