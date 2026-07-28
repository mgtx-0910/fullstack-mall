package com.example.fullstackmall.service.security;

import com.example.fullstackmall.contract.auth.UserRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Spring Security 在一次请求中保存的当前登录用户。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserPrincipal {

    private Long id;
    private String username;
    private String nickname;
    private UserRole role;
}
