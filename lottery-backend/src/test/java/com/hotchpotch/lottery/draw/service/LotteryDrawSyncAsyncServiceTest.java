package com.hotchpotch.lottery.draw.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.hotchpotch.lottery.config.SyncProperties;
import org.junit.jupiter.api.Test;

class LotteryDrawSyncAsyncServiceTest {

    /**
     * 验证异步服务会把历史同步任务交给核心同步服务执行。
     */
    @Test
    void runHistoryTaskDelegatesToSyncService() {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        SyncProperties syncProperties = new SyncProperties();
        syncProperties.setTaskDelayMillis(0);
        LotteryDrawSyncAsyncService asyncService = new LotteryDrawSyncAsyncService(syncService, syncProperties);

        asyncService.runHistoryTask("DLT-HISTORY-ASYNC-001");

        verify(syncService).runHistoryTask("DLT-HISTORY-ASYNC-001");
    }
}
