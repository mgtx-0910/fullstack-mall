package com.example.fullstackmall.contract.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenResponse {

    /** 登录成功后由服务端签发的 JWT，前端后续放入 Authorization 请求头。 */
    private String accessToken;
    /** Token 使用方式，当前固定为 Bearer，前端不要自行拼出其他类型。 */
    private String tokenType;
    /** 访问令牌从签发时刻开始计算的有效秒数，前端可据此安排重新登录。 */
    private long expiresInSeconds;
    /** 与本次令牌对应的安全用户摘要，不包含密码哈希等敏感字段。 */
    private CurrentUserResponse currentUser;
}
