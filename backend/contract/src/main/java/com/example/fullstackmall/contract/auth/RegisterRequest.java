package com.example.fullstackmall.contract.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册请求。password 只在请求链路中短暂存在，绝不能写入日志或响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /** 前端申请的登录用户名；服务端会 trim 后保存，并由数据库唯一约束防止重复。 */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]{4,30}$", message = "用户名只能包含字母、数字、下划线，长度为 4～30")
    private String username;

    /** 前端提交的展示昵称；服务端会去除首尾空白后保存。 */
    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 30, message = "昵称长度必须为 2～30 个字符")
    private String nickname;

    /** 前端提交的明文密码；服务端只保存 BCrypt 哈希，绝不能直接落库。 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度必须为 8～64 个字符")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字")
    private String password;
}
