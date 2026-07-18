package com.hotchpotch.lottery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 用户认证配置。
 */
@ConfigurationProperties(prefix = "lottery.auth")
public class AuthProperties {

    /**
     * 登录会话有效期，单位秒，默认 7 天。
     */
    private long sessionTtlSeconds = 604800;

    public long sessionTtlSeconds() {
        return sessionTtlSeconds;
    }

    public void setSessionTtlSeconds(long sessionTtlSeconds) {
        this.sessionTtlSeconds = sessionTtlSeconds;
    }
}
