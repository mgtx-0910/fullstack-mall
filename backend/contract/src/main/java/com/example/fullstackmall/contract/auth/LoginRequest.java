package com.example.fullstackmall.contract.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /** 前端提交的登录用户名；服务端会按用户名查找账户。 */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 30, message = "用户名不能超过 30 个字符")
    private String username;

    /** 前端提交的明文密码，只用于本次 BCrypt 校验，禁止记录到日志或写入响应。 */
    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码不能超过 64 个字符")
    private String password;
}
