package com.example.fullstackmall.service.health;

import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.contract.health.HealthResponse;
import com.example.fullstackmall.contract.health.IHealthFacade;
import com.example.fullstackmall.service.common.trace.TraceIdContext;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 将 HTTP 请求转换成健康检查用例调用。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Resource
    private IHealthFacade healthFacade;

    @GetMapping
    public ApiResponse<HealthResponse> health(HttpServletRequest request) {
        return ApiResponse.success(
                healthFacade.health(),
                TraceIdContext.get(request)
        );
    }
}
