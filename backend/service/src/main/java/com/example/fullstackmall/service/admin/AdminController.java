package com.example.fullstackmall.service.admin;

import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.service.common.trace.TraceIdContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 最小管理员权限演示接口。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/ping")
    public ApiResponse<Map<String, String>> ping(HttpServletRequest request) {
        return ApiResponse.success(Map.of("message", "admin pong"), TraceIdContext.get(request));
    }
}
