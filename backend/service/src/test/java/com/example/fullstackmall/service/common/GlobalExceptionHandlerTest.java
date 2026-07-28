package com.example.fullstackmall.service.common;

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
class GlobalExceptionHandlerTest {

    @Resource
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldKeepUnknownPathAsNotFoundInsteadOfInternalError() throws Exception {
        mockMvc.perform(get("/api/not-exists"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }
}
