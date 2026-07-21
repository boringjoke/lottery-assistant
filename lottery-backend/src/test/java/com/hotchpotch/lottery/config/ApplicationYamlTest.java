package com.hotchpotch.lottery.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ApplicationYamlTest {

    @Test
    void localRedisDefaultsToLoopbackWithShortTimeout() {
        String yaml = readResource("application.yaml");

        assertThat(yaml).contains("host: ${LOTTERY_REDIS_HOST:127.0.0.1}");
        assertThat(yaml).contains("timeout: ${LOTTERY_REDIS_TIMEOUT:3s}");
        assertThat(yaml).contains("connect-timeout: ${LOTTERY_REDIS_CONNECT_TIMEOUT:3s}");
        assertThat(yaml).doesNotContain("192.168.0.189");
    }

    @Test
    void favoriteMaxActiveCountDefaultsToOneHundred() {
        String yaml = readResource("application.yaml");

        assertThat(yaml).contains("max-active-count: ${LOTTERY_FAVORITE_MAX_ACTIVE_COUNT:100}");
    }

    @Test
    void autoLatestSyncDefaultsToEnabledAndSupportsCronOverride() {
        String yaml = readResource("application.yaml");

        assertThat(yaml).contains("auto-latest:");
        assertThat(yaml).contains("enabled: ${LOTTERY_SYNC_AUTO_LATEST_ENABLED:true}");
        assertThat(yaml).contains("cron: ${LOTTERY_SYNC_AUTO_LATEST_CRON:");
        assertThat(yaml).contains("zone: ${LOTTERY_SYNC_AUTO_LATEST_ZONE:Asia/Shanghai}");
    }

    private String readResource(String resourcePath) {
        URL resource = getClass().getClassLoader().getResource(resourcePath);

        assertThat(resource)
                .as("配置文件应放在 src/main/resources/%s", resourcePath)
                .isNotNull();

        try {
            return new String(resource.openStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("读取配置文件失败: " + resourcePath, ex);
        }
    }
}
