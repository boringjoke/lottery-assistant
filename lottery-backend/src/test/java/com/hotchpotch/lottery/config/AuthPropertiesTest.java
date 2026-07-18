package com.hotchpotch.lottery.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class AuthPropertiesTest {

    /**
     * 验证认证会话默认保留 7 天。
     */
    @Test
    void defaultAuthPropertiesUseSevenDaysSessionTtl() {
        AuthProperties properties = new AuthProperties();

        assertThat(properties.sessionTtlSeconds()).isEqualTo(604800);
    }

    /**
     * 验证认证配置可以从 application.yaml 或环境变量绑定。
     */
    @Test
    void bindsAuthPropertiesFromConfiguration() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource();
        source.put("lottery.auth.session-ttl-seconds", "3600");

        AuthProperties properties = new Binder(source)
                .bind("lottery.auth", AuthProperties.class)
                .orElseThrow(IllegalStateException::new);

        assertThat(properties.sessionTtlSeconds()).isEqualTo(3600);
    }
}
