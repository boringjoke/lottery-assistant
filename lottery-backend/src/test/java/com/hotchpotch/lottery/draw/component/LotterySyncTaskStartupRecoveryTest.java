package com.hotchpotch.lottery.draw.component;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.hotchpotch.lottery.draw.service.LotteryDrawSyncService;
import org.junit.jupiter.api.Test;

class LotterySyncTaskStartupRecoveryTest {

    /**
     * 验证启动恢复组件会调用同步服务处理遗留未完成任务。
     */
    @Test
    void recoverInterruptedActiveTasksDelegatesToSyncService() {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        LotterySyncTaskStartupRecovery recovery = new LotterySyncTaskStartupRecovery(syncService);

        recovery.recoverInterruptedActiveTasks();

        verify(syncService).recoverInterruptedActiveTasks();
    }
}
