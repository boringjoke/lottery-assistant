package com.hotchpotch.lottery.user.security;

import com.hotchpotch.lottery.user.record.AuthSession;
import com.hotchpotch.lottery.user.service.AuthSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 将 opaque token 会话转换为 Spring Security 认证上下文。
 */
public class AuthTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_COOKIE_NAME = "LOTTERY_AUTH_TOKEN";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthSessionService authSessionService;

    public AuthTokenAuthenticationFilter(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    /**
     * 每个请求只解析一次 Bearer 或 Cookie token，找到有效会话后写入 SecurityContext。
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            resolveToken(request).ifPresent(token -> authSessionService.findSession(token)
                    .ifPresent(session -> authenticate(request, session)));
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中解析 token；Authorization Bearer 优先于 Cookie。
     */
    private java.util.Optional<String> resolveToken(HttpServletRequest request) {
        String bearerToken = resolveBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (bearerToken != null) {
            return java.util.Optional.of(bearerToken);
        }

        return java.util.Optional.ofNullable(resolveCookieToken(request));
    }

    /**
     * 从 Authorization 请求头中提取 Bearer token。
     */
    private String resolveBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return trimToNull(authorization.substring(BEARER_PREFIX.length()));
    }

    /**
     * 从认证 Cookie 中提取 token。
     */
    private String resolveCookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                return trimToNull(cookie.getValue());
            }
        }

        return null;
    }

    /**
     * 将有效会话写入 Spring Security 上下文。
     */
    private void authenticate(HttpServletRequest request, AuthSession session) {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(
                session.userId(),
                session.nickname(),
                session.avatarUrl(),
                session.roles());
        List<SimpleGrantedAuthority> authorities = session.roles().stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, session.token(), authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 去除 token 前后空白；空 token 统一视为不存在。
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
