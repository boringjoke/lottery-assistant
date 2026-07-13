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
     * 单个历史同步任务最多允许同步的页数。
     */
    private int maxPagesPerTask = 20;

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
}
