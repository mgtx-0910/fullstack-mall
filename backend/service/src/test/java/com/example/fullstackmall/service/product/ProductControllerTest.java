package com.example.fullstackmall.service.product;

import com.example.fullstackmall.service.FullstackMallApplication;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FullstackMallApplication.class)
@AutoConfigureMockMvc
@Transactional
class ProductControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ProductDbService productDbService;

    @Test
    void shouldExposeOnlyOnSaleProductsToAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("status", "OFF_SHELF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[*].status", everyItem(is("ON_SALE"))));
    }

    @Test
    void shouldHideDraftProductDetailFromAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/products/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldAllowAdminToCreateDraftWithTrustedCreatorId() throws Exception {
        String adminToken = loginAndGetToken("admin", "Admin123456");

        mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 1,
                                  "title": "  MacBook Air M3  ",
                                  "description": "  自用笔记本，包装齐全。  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("MacBook Air M3"))
                .andExpect(jsonPath("$.data.description").value("自用笔记本，包装齐全。"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.createdBy").value(5));

        ProductEntity saved = productDbService.lambdaQuery()
                .eq(ProductEntity::getTitle, "MacBook Air M3")
                .one();
        assertThat(saved).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo(5L);
        assertThat(saved.getStatusCode()).isEqualTo("DRAFT");
    }

    @Test
    void shouldRejectInvalidCreateRequest() throws Exception {
        String adminToken = loginAndGetToken("admin", "Admin123456");

        mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 0, "title": " ", "description": null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn404WhenCategoryDoesNotExistOrIsDisabled() throws Exception {
        String adminToken = loginAndGetToken("admin", "Admin123456");

        mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 3, "title": "禁用分类商品", "description": null}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void shouldRejectUnauthenticatedAndNormalUserCreateRequests() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        String userToken = loginAndGetToken("xiaoming", "Mall123456");
        mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldAllowDraftToGoOnSale() throws Exception {
        String adminToken = loginAndGetToken("admin", "Admin123456");

        mockMvc.perform(patch("/api/admin/products/2/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus":"ON_SALE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"));

        assertThat(productDbService.getById(2L).getStatusCode()).isEqualTo("ON_SALE");
    }

    @Test
    void shouldRejectIllegalStatusTransition() throws Exception {
        String adminToken = loginAndGetToken("admin", "Admin123456");

        mockMvc.perform(patch("/api/admin/products/1/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus":"ON_SALE"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_PRODUCT_STATUS_TRANSITION"));
    }

    @Test
    void shouldFilterAdminProductsByStatus() throws Exception {
        String adminToken = loginAndGetToken("admin", "Admin123456");

        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(2))
                .andExpect(jsonPath("$.data.records[0].status").value("DRAFT"));
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

    private String validCreateBody() {
        return """
                {"categoryId": 1, "title": "测试商品", "description": "测试描述"}
                """;
    }
}
