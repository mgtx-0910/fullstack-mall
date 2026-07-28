package com.example.fullstackmall.service.auth;

import com.example.fullstackmall.contract.auth.AuthTokenResponse;
import com.example.fullstackmall.contract.auth.CurrentUserResponse;
import com.example.fullstackmall.contract.auth.IAuthFacade;
import com.example.fullstackmall.contract.auth.LoginRequest;
import com.example.fullstackmall.contract.auth.RegisterRequest;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

/**
 * 认证契约的 Facade 实现。
 *
 * <p>Facade 是 Controller 与内部业务服务之间的边界：对外实现 contract 中的稳定接口，
 * 对内把认证工作交给 {@link AuthService}，避免 Controller 直接依赖密码、JWT 等实现细节。</p>
 */
@Service
public class AuthFacade implements IAuthFacade {

    @Resource
    private AuthService authService;

    @Override
    public CurrentUserResponse register(RegisterRequest request) {
        return authService.register(request);
    }

    @Override
    public AuthTokenResponse login(LoginRequest request) {
        return authService.login(request);
    }

    @Override
    public CurrentUserResponse currentUser() {
        return authService.currentUser();
    }
}
