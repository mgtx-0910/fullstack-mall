package com.example.fullstackmall.service.category;

import com.example.fullstackmall.service.FullstackMallApplication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = FullstackMallApplication.class)
@AutoConfigureMockMvc
class CategoryControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private ObjectMapper objectMapper;

    @Test
    void shouldValidateAndNormalizeCategoryDraft() throws Exception {
        mockMvc.perform(post("/api/categories/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  手机数码  ",
                                  "description": "  二手手机及数码设备  ",
                                  "sortOrder": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("手机数码"))
                .andExpect(jsonPath("$.data.description").value("二手手机及数码设备"))
                .andExpect(jsonPath("$.data.sortOrder").value(10))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    void shouldReturnFieldErrorsWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/categories/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "description": "正常描述",
                                  "sortOrder": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.fieldErrors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.data.fieldErrors[*].field", hasItem("sortOrder")))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldReturnConflictWhenCategoryNameIsReserved() throws Exception {
        mockMvc.perform(post("/api/categories/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "全部",
                                  "description": null,
                                  "sortOrder": 0
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_NAME_RESERVED"))
                .andExpect(jsonPath("$.message").value("分类名称“全部”是系统保留名称"));
    }

    @Test
    void shouldReturnBadRequestWhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/categories/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"手机数码\", \"sortOrder\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    }

    @Test
    void shouldGenerateNewTraceIdWhenRequestHeaderIsInvalid() throws Exception {
        String invalidTraceId = "bad trace id!";

        MvcResult result = mockMvc.perform(post("/api/categories/validate")
                        .header("X-Trace-Id", invalidTraceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "图书",
                                  "description": "二手书籍",
                                  "sortOrder": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String responseTraceId = result.getResponse().getHeader("X-Trace-Id");
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue(responseTraceId != null && responseTraceId.matches("[A-Za-z0-9_-]{8,64}"));
        assertNotEquals(invalidTraceId, responseTraceId);
        assertEquals(responseTraceId, body.get("traceId").asText());
    }

    @Test
    void shouldReuseValidTraceIdInHeaderAndBody() throws Exception {
        String traceId = "frontend-request-20260712";

        mockMvc.perform(post("/api/categories/validate")
                        .header("X-Trace-Id", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "图书",
                                  "description": "二手书籍",
                                  "sortOrder": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", traceId))
                .andExpect(jsonPath("$.traceId").value(traceId));
    }
}
