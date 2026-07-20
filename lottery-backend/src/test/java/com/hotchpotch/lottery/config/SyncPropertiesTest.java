package com.hotchpotch.lottery.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class SyncPropertiesTest {

    /**
     * 验证同步任务默认使用单线程和保守的源站保护参数。
     */
    @Test
    void defaultSyncPropertiesUseSingleWorkerAndSafeDelays() {
        SyncProperties properties = new SyncProperties();

        assertThat(properties.corePoolSize()).isEqualTo(1);
        assertThat(properties.maxPoolSize()).isEqualTo(1);
        assertThat(properties.queueCapacity()).isEqualTo(10);
        assertThat(properties.defaultPageDelayMillis()).isEqualTo(2000);
        assertThat(properties.taskDelayMillis()).isEqualTo(10000);
        assertThat(properties.maxPagesPerTask()).isEqualTo(20);
        assertThat(properties.autoLatest().enabled()).isFalse();
        assertThat(properties.autoLatest().cron()).isEqualTo("0 45 21 ? * MON,WED,SAT");
        assertThat(properties.autoLatest().zone()).isEqualTo("Asia/Shanghai");
    }

    /**
     * 验证同步任务配置可以从 application.yaml 或环境变量绑定。
     */
    @Test
    void bindsSyncPropertiesFromConfiguration() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource();
        source.put("lottery.sync.core-pool-size", "1");
        source.put("lottery.sync.max-pool-size", "1");
        source.put("lottery.sync.queue-capacity", "5");
        source.put("lottery.sync.default-page-delay-millis", "1500");
        source.put("lottery.sync.task-delay-millis", "8000");
        source.put("lottery.sync.max-pages-per-task", "12");
        source.put("lottery.sync.auto-latest.enabled", "true");
        source.put("lottery.sync.auto-latest.cron", "0 50 21 ? * MON,WED,SAT");
        source.put("lottery.sync.auto-latest.zone", "Asia/Shanghai");

        SyncProperties properties = new Binder(source)
                .bind("lottery.sync", SyncProperties.class)
                .orElseThrow(IllegalStateException::new);

        assertThat(properties.corePoolSize()).isEqualTo(1);
        assertThat(properties.maxPoolSize()).isEqualTo(1);
        assertThat(properties.queueCapacity()).isEqualTo(5);
        assertThat(properties.defaultPageDelayMillis()).isEqualTo(1500);
        assertThat(properties.taskDelayMillis()).isEqualTo(8000);
        assertThat(properties.maxPagesPerTask()).isEqualTo(12);
        assertThat(properties.autoLatest().enabled()).isTrue();
        assertThat(properties.autoLatest().cron()).isEqualTo("0 50 21 ? * MON,WED,SAT");
        assertThat(properties.autoLatest().zone()).isEqualTo("Asia/Shanghai");
    }
}
