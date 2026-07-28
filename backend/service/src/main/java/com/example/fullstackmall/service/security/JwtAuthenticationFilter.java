package com.example.fullstackmall.service.security;

import com.example.fullstackmall.contract.auth.UserRole;
import com.example.fullstackmall.service.auth.JwtTokenService;
import com.example.fullstackmall.service.user.entity.UserEntity;
import com.example.fullstackmall.service.user.service.UserDbService;

import io.jsonwebtoken.JwtException;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 把 Bearer JWT 转换为 Spring Security 能识别的 Authentication。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Resource
    private JwtTokenService jwtTokenService;

    @Resource
    private UserDbService userDbService;

    @Resource
    private RestAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        // 没有 Token 时先继续执行，由后面的 URL 权限规则决定该接口是否允许匿名访问。
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            if (token.isEmpty()) {
                throw new BadCredentialsException("Bearer Token 为空");
            }
            Long userId = jwtTokenService.parseUserId(token);
            // 即使 JWT 尚未过期，也重新读取数据库，确保停用账号立即失去访问权限。
            UserEntity user = userDbService.findUserById(userId)
                    .filter(candidate -> Integer.valueOf(1).equals(candidate.getStatus()))
                    .orElseThrow(() -> new BadCredentialsException("Token 对应用户不存在或已停用"));

            UserRole role = UserRole.fromCode(user.getRoleCode());
            CurrentUserPrincipal principal = new CurrentUserPrincipal(
                    user.getId(), user.getUsername(), user.getNickname(), role
            );
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            );
            // 放入 SecurityContext 后，Controller、Facade 和权限表达式都能取得当前用户。
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException | BadCredentialsException exception) {
            // 认证失败必须清空上下文，防止线程复用时误带上旧身份。
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Access Token 无效", exception)
            );
        }
    }
}
