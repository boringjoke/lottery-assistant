package com.hotchpotch.lottery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 开奖同步任务执行配置。
 */
@ConfigurationProperties(prefix = "lottery.sync")
public class SyncProperties {

    /**
     * 同步任务核心线程数，默认单线程保护 crawler 和源站。
     */
    private int corePoolSize = 1;

    /**
     * 同步任务最大线程数，默认单线程避免并发抓取。
     */
    private int maxPoolSize = 1;

    /**
     * 同步任务等待队列容量。
     */
    private int queueCapacity = 10;

    /**
     * 默认每页同步间隔，单位毫秒。
     */
    private int defaultPageDelayMillis = 2000;

    /**
     * 不同同步任务之间的间隔，单位毫秒。
     */
    private int taskDelayMillis = 10000;

    /**
     * 单个历史或范围同步任务最多允许扫描的页数。
     */
    private int maxPagesPerTask = 20;

    /**
     * 自动同步最新开奖配置。
     */
    private AutoLatest autoLatest = new AutoLatest();

    public int corePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int maxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int queueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int defaultPageDelayMillis() {
        return defaultPageDelayMillis;
    }

    public void setDefaultPageDelayMillis(int defaultPageDelayMillis) {
        this.defaultPageDelayMillis = defaultPageDelayMillis;
    }

    public int taskDelayMillis() {
        return taskDelayMillis;
    }

    public void setTaskDelayMillis(int taskDelayMillis) {
        this.taskDelayMillis = taskDelayMillis;
    }

    public int maxPagesPerTask() {
        return maxPagesPerTask;
    }

    public void setMaxPagesPerTask(int maxPagesPerTask) {
        this.maxPagesPerTask = maxPagesPerTask;
    }

    public AutoLatest autoLatest() {
        return autoLatest;
    }

    public void setAutoLatest(AutoLatest autoLatest) {
        this.autoLatest = autoLatest;
    }

    /**
     * 最新开奖自动同步配置，默认关闭，避免本地启动后自动请求 crawler。
     */
    public static class AutoLatest {

        /**
         * 是否启用最新开奖自动同步。
         */
        private boolean enabled = false;

        /**
         * 最新开奖自动同步 cron 表达式，默认在大乐透开奖日稍晚触发。
         */
        private String cron = "0 45 21 ? * MON,WED,SAT";

        /**
         * cron 表达式使用的时区。
         */
        private String zone = "Asia/Shanghai";

        public boolean enabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String cron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public String zone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }
    }
}
