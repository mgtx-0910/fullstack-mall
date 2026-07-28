package com.example.fullstackmall.service.auth;

import com.example.fullstackmall.service.user.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 的唯一创建和解析入口，避免各处重复处理密钥与 Claims。
 */
@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final long expirationSeconds;
    private final Clock clock;

    public JwtTokenService(
            @Value("${security.jwt.secret}") String base64Secret,
            @Value("${security.jwt.expiration-seconds:1800}") long expirationSeconds
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationSeconds = expirationSeconds;
        this.clock = Clock.systemUTC();
    }

    /**
     * 创建访问令牌。subject 只保存稳定的用户 ID，角色等信息仍以数据库当前值为准。
     */
    public AccessToken createAccessToken(UserEntity user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds);

        String token = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRoleCode())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();

        return new AccessToken(token, expirationSeconds);
    }

    /**
     * 校验签名和过期时间后提取用户 ID；令牌非法时由 JJWT 抛异常交给 Filter 统一处理。
     */
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }
}
