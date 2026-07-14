package com.hotchpotch.lottery.draw.service;

import com.hotchpotch.lottery.config.SyncProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 开奖同步任务执行服务。
 */
@Service
public class LotteryDrawSyncTaskService {

    private final LotteryDrawSyncService syncService;
    private final SyncProperties syncProperties;

    /**
     * 初始化任务执行服务依赖的同步服务和同步配置。
     */
    public LotteryDrawSyncTaskService(LotteryDrawSyncService syncService, SyncProperties syncProperties) {
        this.syncService = syncService;
        this.syncProperties = syncProperties;
    }

    /**
     * 在线程池中执行历史同步任务，并在任务结束后等待一段时间保护 crawler 和源站。
     */
    @Async("lotterySyncTaskExecutor")
    public void runHistoryTask(String taskNo) {
        try {
            syncService.runHistoryTask(taskNo);
        } finally {
            sleepTaskDelay();
        }
    }

    /**
     * 在线程池中按任务类型执行同步任务，并在任务结束后等待一段时间保护 crawler 和源站。
     */
    @Async("lotterySyncTaskExecutor")
    public void runTask(String taskNo) {
        try {
            syncService.runTask(taskNo);
        } finally {
            sleepTaskDelay();
        }
    }

    /**
     * 等待不同同步任务之间的保护间隔。
     */
    private void sleepTaskDelay() {
        int delayMillis = syncProperties.taskDelayMillis();
        if (delayMillis <= 0) {
            return;
        }

        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
