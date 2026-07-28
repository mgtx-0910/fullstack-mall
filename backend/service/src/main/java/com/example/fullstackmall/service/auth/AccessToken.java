package com.example.fullstackmall.service.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * service 内部的 Token 签发结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessToken {

    private String value;
    private long expiresInSeconds;
}
