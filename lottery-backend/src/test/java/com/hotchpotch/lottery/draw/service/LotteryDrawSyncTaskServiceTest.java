package com.hotchpotch.lottery.draw.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.hotchpotch.lottery.config.SyncProperties;
import org.junit.jupiter.api.Test;

class LotteryDrawSyncTaskServiceTest {

    /**
     * 验证异步服务会把历史同步任务交给核心同步服务执行。
     */
    @Test
    void runHistoryTaskDelegatesToSyncService() {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        SyncProperties syncProperties = new SyncProperties();
        syncProperties.setTaskDelayMillis(0);
        LotteryDrawSyncTaskService taskService = new LotteryDrawSyncTaskService(syncService, syncProperties);

        taskService.runHistoryTask("DLT-HISTORY-ASYNC-001");

        verify(syncService).runHistoryTask("DLT-HISTORY-ASYNC-001");
    }

    /**
     * 验证异步服务会把通用同步任务交给核心同步服务按类型分发执行。
     */
    @Test
    void runTaskDelegatesToSyncService() {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        SyncProperties syncProperties = new SyncProperties();
        syncProperties.setTaskDelayMillis(0);
        LotteryDrawSyncTaskService taskService = new LotteryDrawSyncTaskService(syncService, syncProperties);

        taskService.runTask("DLT-ISSUE-RANGE-ASYNC-001");

        verify(syncService).runTask("DLT-ISSUE-RANGE-ASYNC-001");
    }
}
