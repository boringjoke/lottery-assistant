package com.hotchpotch.lottery.user.controller;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.common.response.ApiResponse;
import com.hotchpotch.lottery.config.AuthProperties;
import com.hotchpotch.lottery.user.record.AuthSession;
import com.hotchpotch.lottery.user.record.CurrentUserResponse;
import com.hotchpotch.lottery.user.record.LoginResponse;
import com.hotchpotch.lottery.user.record.PasswordLoginRequest;
import com.hotchpotch.lottery.user.service.AuthSessionService;
import com.hotchpotch.lottery.user.service.UserAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String AUTH_COOKIE_NAME = "LOTTERY_AUTH_TOKEN";
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserAuthService userAuthService;
    private final AuthSessionService authSessionService;
    private final AuthProperties authProperties;

    public AuthController(
            UserAuthService userAuthService,
            AuthSessionService authSessionService,
            AuthProperties authProperties) {
        this.userAuthService = userAuthService;
        this.authSessionService = authSessionService;
        this.authProperties = authProperties;
    }

    /**
     * 账号密码登录，创建服务端会话并写入 HttpOnly Cookie。
     */
    @PostMapping("/login")
    public ApiResponse<AuthSession> login(@RequestBody PasswordLoginRequest request, HttpServletResponse response) {
        LoginResponse loginResponse = userAuthService.loginWithPassword(request);
        AuthSession session = authSessionService.createSession(loginResponse);
        addAuthCookie(response, session.token(), Duration.ofSeconds(authProperties.sessionTtlSeconds()));

        return ApiResponse.success(session);
    }

    /**
     * 查询当前登录用户。
     */
    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> currentUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            HttpServletRequest request) {
        AuthSession session = findRequiredSession(authorization, request);

        return ApiResponse.success(new CurrentUserResponse(
                session.userId(),
                session.nickname(),
                session.avatarUrl(),
                session.roles()));
    }

    /**
     * 退出登录，删除服务端会话并清理 Cookie。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            HttpServletRequest request,
            HttpServletResponse response) {
        String token = resolveToken(authorization, request);
        if (token != null) {
            authSessionService.deleteSession(token);
        }
        clearAuthCookie(response);

        return ApiResponse.success(null);
    }

    /**
     * 根据请求中的 token 查询有效会话；不存在时返回未登录。
     */
    private AuthSession findRequiredSession(String authorization, HttpServletRequest request) {
        String token = resolveToken(authorization, request);
        if (token == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return authSessionService.findSession(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 优先从 Authorization Bearer 读取 token，其次读取认证 Cookie。
     */
    private String resolveToken(String authorization, HttpServletRequest request) {
        String bearerToken = resolveBearerToken(authorization);
        if (bearerToken != null) {
            return bearerToken;
        }

        return resolveCookieToken(request);
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
     * 从 Cookie 中提取 Web 登录 token。
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
     * 写入 HttpOnly 认证 Cookie。
     */
    private void addAuthCookie(HttpServletResponse response, String token, Duration maxAge) {
        addSetCookieHeader(response, ResponseCookie.from(AUTH_COOKIE_NAME, token)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAge)
                .build());
    }

    /**
     * 清理浏览器中的认证 Cookie。
     */
    private void clearAuthCookie(HttpServletResponse response) {
        addSetCookieHeader(response, ResponseCookie.from(AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build());
    }

    /**
     * 在当前响应中追加 Set-Cookie 头。
     */
    private void addSetCookieHeader(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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
