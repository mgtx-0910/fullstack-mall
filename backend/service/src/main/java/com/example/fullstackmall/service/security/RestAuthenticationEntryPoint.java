package com.example.fullstackmall.service.security;

import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.service.common.trace.TraceIdContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未登录或 Token 无效时的 401 JSON 响应入口；Security 异常发生在 Controller 之前。
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(
                ApiCode.UNAUTHORIZED,
                ApiCode.UNAUTHORIZED.defaultMessage(),
                null,
                TraceIdContext.get(request)
        ));
    }
}
