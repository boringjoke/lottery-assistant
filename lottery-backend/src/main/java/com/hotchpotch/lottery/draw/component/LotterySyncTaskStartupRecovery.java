package com.hotchpotch.lottery.draw.component;

import com.hotchpotch.lottery.draw.service.LotteryDrawSyncService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 应用启动后恢复同步任务状态，避免重启前遗留的 PENDING/RUNNING 任务长期卡住。
 */
@Component
public class LotterySyncTaskStartupRecovery {

    private final LotteryDrawSyncService syncService;

    public LotterySyncTaskStartupRecovery(LotteryDrawSyncService syncService) {
        this.syncService = syncService;
    }

    /**
     * Spring 应用准备就绪后，将上次未完成的同步任务标记为失败。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedActiveTasks() {
        syncService.recoverInterruptedActiveTasks();
    }
}
