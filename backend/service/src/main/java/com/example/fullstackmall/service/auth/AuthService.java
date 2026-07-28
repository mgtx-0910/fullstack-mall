package com.example.fullstackmall.service.auth;

import com.example.fullstackmall.contract.auth.AuthTokenResponse;
import com.example.fullstackmall.contract.auth.CurrentUserResponse;
import com.example.fullstackmall.contract.auth.LoginRequest;
import com.example.fullstackmall.contract.auth.RegisterRequest;
import com.example.fullstackmall.contract.auth.UserRole;
import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.security.CurrentUserPrincipal;
import com.example.fullstackmall.service.user.entity.UserEntity;
import com.example.fullstackmall.service.user.service.UserDbService;

import jakarta.annotation.Resource;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 注册和登录的业务规则。
 */
@Service
public class AuthService {

    /** 数据库中 1 表示账号可用，集中定义避免业务代码散落魔法数字。 */
    private static final int USER_ENABLED = 1;

    @Resource
    private UserDbService userDbService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private JwtTokenService jwtTokenService;

    public CurrentUserResponse register(RegisterRequest request) {
        // 用户名统一转小写，避免 Alice 和 alice 被当成两个账号。
        String username = normalizeUsername(request.getUsername());
        // 先查询可以尽早返回友好错误，但最终仍要依赖数据库唯一约束防并发重复注册。
        if (userDbService.usernameExists(username)) {
            throw new BusinessException(ApiCode.USERNAME_ALREADY_EXISTS, ApiCode.USERNAME_ALREADY_EXISTS.defaultMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setNickname(request.getNickname().trim());
        // 数据库只保存 BCrypt 哈希，绝不能保存或返回用户原始密码。
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRoleCode(UserRole.USER.name());
        user.setStatus(USER_ENABLED);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        try {
            userDbService.save(user);
        } catch (DuplicateKeyException exception) {
            // 两个请求可能同时通过前面的 exists 查询，唯一索引是最后一道并发防线。
            throw new BusinessException(ApiCode.USERNAME_ALREADY_EXISTS, ApiCode.USERNAME_ALREADY_EXISTS.defaultMessage());
        }
        return toResponse(user);
    }

    public AuthTokenResponse login(LoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        UserEntity user = userDbService.findByUsername(username)
                .orElseThrow(this::invalidCredentials);

        // 用户不存在和密码错误统一返回同一个业务码，避免泄露“某用户名是否存在”。
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (!Integer.valueOf(USER_ENABLED).equals(user.getStatus())) {
            throw new BusinessException(
                    ApiCode.ACCOUNT_DISABLED,
                    ApiCode.ACCOUNT_DISABLED.defaultMessage(),
                    HttpStatus.FORBIDDEN
            );
        }

        AccessToken token = jwtTokenService.createAccessToken(user);
        return new AuthTokenResponse(
                token.getValue(),
                "Bearer",
                token.getExpiresInSeconds(),
                toResponse(user)
        );
    }

    public CurrentUserResponse currentUser() {
        // JWT Filter 已把登录用户放入 SecurityContext，这里只读取可信身份，不接受前端传 userId。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, ApiCode.UNAUTHORIZED.defaultMessage(), HttpStatus.UNAUTHORIZED);
        }
        return new CurrentUserResponse(principal.getId(), principal.getUsername(), principal.getNickname(), principal.getRole());
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(
                ApiCode.INVALID_CREDENTIALS,
                ApiCode.INVALID_CREDENTIALS.defaultMessage(),
                HttpStatus.UNAUTHORIZED
        );
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private CurrentUserResponse toResponse(UserEntity user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                UserRole.fromCode(user.getRoleCode())
        );
    }
}
