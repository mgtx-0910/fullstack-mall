package com.example.fullstackmall.contract.auth;

/**
 * 认证能力对外契约。
 */
public interface IAuthFacade {

    /**
     * 注册普通用户；密码会在 service 中转为 BCrypt 哈希后再写库。
     *
     * @param request 注册信息
     * @return 注册成功后的安全用户信息
     */
    CurrentUserResponse register(RegisterRequest request);

    /**
     * 校验用户名和密码并签发 JWT。
     *
     * @param request 登录凭据
     * @return 令牌和当前用户信息
     */
    AuthTokenResponse login(LoginRequest request);

    /**
     * 读取已经由 JWT Filter 放入 SecurityContext 的当前登录用户。
     *
     * @return 当前登录用户信息
     */
    CurrentUserResponse currentUser();
}
