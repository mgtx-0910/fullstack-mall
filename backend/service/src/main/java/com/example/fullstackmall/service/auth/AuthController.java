package com.example.fullstackmall.service.auth;

import com.example.fullstackmall.contract.auth.AuthTokenResponse;
import com.example.fullstackmall.contract.auth.CurrentUserResponse;
import com.example.fullstackmall.contract.auth.IAuthFacade;
import com.example.fullstackmall.contract.auth.LoginRequest;
import com.example.fullstackmall.contract.auth.RegisterRequest;
import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.service.common.trace.TraceIdContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 HTTP 入口。
 *
 * <p>Controller 只负责接收并校验 HTTP 参数、调用 Facade、包装统一响应；
 * 密码校验、JWT 生成和当前用户解析等业务逻辑由后面的认证服务负责。</p>
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证")
public class AuthController {

    @Resource
    private IAuthFacade authFacade;

    @PostMapping("/register")
    @Operation(summary = "注册普通用户")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest
    ) {
        CurrentUserResponse data = authFacade.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, TraceIdContext.get(servletRequest)));
    }

    @PostMapping("/login")
    @Operation(summary = "用户名密码登录")
    public ApiResponse<AuthTokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(authFacade.login(request), TraceIdContext.get(servletRequest));
    }

    @GetMapping("/me")
    @Operation(summary = "查询当前登录用户")
    public ApiResponse<CurrentUserResponse> currentUser(HttpServletRequest servletRequest) {
        return ApiResponse.success(authFacade.currentUser(), TraceIdContext.get(servletRequest));
    }
}
