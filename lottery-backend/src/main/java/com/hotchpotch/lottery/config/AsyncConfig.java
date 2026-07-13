package com.hotchpotch.lottery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 后台异步任务配置。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 创建彩票开奖同步专用执行器，默认单线程串行执行，避免并发请求 crawler。
     */
    @Bean("lotterySyncTaskExecutor")
    public ThreadPoolTaskExecutor lotterySyncTaskExecutor(SyncProperties syncProperties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(syncProperties.corePoolSize());
        executor.setMaxPoolSize(syncProperties.maxPoolSize());
        executor.setQueueCapacity(syncProperties.queueCapacity());
        executor.setThreadNamePrefix("lottery-sync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
