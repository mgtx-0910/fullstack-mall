package com.example.fullstackmall.service.user;

import com.example.fullstackmall.service.FullstackMallApplication;

import jakarta.annotation.Resource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = FullstackMallApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class UserControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Test
    void shouldReturnFirstPageWithDefaultParameters() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.pages").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(5))
                .andExpect(jsonPath("$.data.records[0].username").value("admin"));
    }

    @Test
    void shouldFilterByKeywordAndStatus() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("keyword", "  小  ")
                        .param("status", "1")
                        .param("pageNum", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.pages").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value(1));
    }

    @Test
    void shouldReturnEmptyPageWhenNoUserMatches() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("keyword", "不存在的用户"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    @Test
    void shouldRejectOversizedPage() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.fieldErrors[0].field").value("pageSize"));
    }

    @Test
    void shouldRejectNonNumericPageNumberWithUnifiedResponse() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("pageNum", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.fieldErrors[0].field").value("pageNum"));
    }
}
