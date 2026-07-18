package com.hotchpotch.lottery.user.service;

import com.hotchpotch.lottery.config.AuthProperties;
import com.hotchpotch.lottery.user.record.AuthSession;
import com.hotchpotch.lottery.user.record.LoginResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 用户 opaque token 会话服务。
 */
@Service
public class AuthSessionService {

    private static final String SESSION_KEY_PREFIX = "lottery:auth:session:";
    private static final String SESSION_VALUE_VERSION = "v1";
    private static final int TOKEN_BYTES = 32;

    private final StringRedisTemplate redisTemplate;
    private final AuthProperties authProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthSessionService(StringRedisTemplate redisTemplate, AuthProperties authProperties) {
        this.redisTemplate = redisTemplate;
        this.authProperties = authProperties;
    }

    /**
     * 创建登录会话，并写入 Redis。
     */
    public AuthSession createSession(LoginResponse loginResponse) {
        String token = generateToken();
        AuthSession session = new AuthSession(
                token,
                loginResponse.userId(),
                loginResponse.nickname(),
                loginResponse.avatarUrl(),
                List.copyOf(loginResponse.roles()),
                LocalDateTime.now().plusSeconds(authProperties.sessionTtlSeconds()));

        redisTemplate.opsForValue().set(
                sessionKey(token),
                serialize(session),
                Duration.ofSeconds(authProperties.sessionTtlSeconds()));

        return session;
    }

    /**
     * 按 token 查询登录会话。
     */
    public Optional<AuthSession> findSession(String token) {
        String normalizedToken = trimToNull(token);
        if (normalizedToken == null) {
            return Optional.empty();
        }

        String key = sessionKey(normalizedToken);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(deserialize(value));
        } catch (RuntimeException ex) {
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }

    /**
     * 删除登录会话。
     */
    public void deleteSession(String token) {
        String normalizedToken = trimToNull(token);
        if (normalizedToken == null) {
            return;
        }

        redisTemplate.delete(sessionKey(normalizedToken));
    }

    /**
     * 生成不携带业务含义的随机 token。
     */
    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 生成 Redis 会话 key。
     */
    private String sessionKey(String token) {
        return SESSION_KEY_PREFIX + token;
    }

    /**
     * 将会话序列化为 Redis 内部存储格式。
     */
    private String serialize(AuthSession session) {
        return String.join(
                "|",
                SESSION_VALUE_VERSION,
                session.token(),
                String.valueOf(session.userId()),
                encode(session.nickname()),
                encode(session.avatarUrl()),
                encode(String.join(",", session.roles())),
                session.expireTime().toString());
    }

    /**
     * 将 Redis 内部存储格式还原为会话对象。
     */
    private AuthSession deserialize(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 7 || !SESSION_VALUE_VERSION.equals(parts[0])) {
            throw new IllegalArgumentException("认证会话格式不合法");
        }

        List<String> roles = decode(parts[5]).isBlank()
                ? List.of()
                : Arrays.stream(decode(parts[5]).split(","))
                        .filter(role -> !role.isBlank())
                        .toList();
        return new AuthSession(
                parts[1],
                Long.valueOf(parts[2]),
                decode(parts[3]),
                decode(parts[4]),
                roles,
                LocalDateTime.parse(parts[6]));
    }

    /**
     * 对可能包含分隔符的会话字段做 URL 安全 Base64 编码。
     */
    private String encode(String value) {
        String safeValue = value == null ? "" : value;
        return Base64.getUrlEncoder().encodeToString(safeValue.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 还原会话字段的 URL 安全 Base64 编码。
     */
    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
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
