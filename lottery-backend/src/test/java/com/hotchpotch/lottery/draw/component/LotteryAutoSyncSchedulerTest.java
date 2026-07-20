package com.hotchpotch.lottery.draw.component;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.config.SyncProperties;
import com.hotchpotch.lottery.draw.service.LotteryDrawSyncService;
import org.junit.jupiter.api.Test;

class LotteryAutoSyncSchedulerTest {

    /**
     * 验证自动同步关闭时不会触发 crawler 同步链路。
     */
    @Test
    void syncLatestDrawDoesNotTriggerServiceWhenDisabled() {
        LotteryDrawSyncService syncService = org.mockito.Mockito.mock(LotteryDrawSyncService.class);
        SyncProperties syncProperties = new SyncProperties();
        LotteryAutoSyncScheduler scheduler = new LotteryAutoSyncScheduler(
                syncService,
                syncProperties);

        scheduler.syncLatestDraw();

        verify(syncService, never()).syncLatestDraw("SCHEDULED");
    }

    /**
     * 验证自动同步开启时使用 SCHEDULED 来源触发最新开奖同步。
     */
    @Test
    void syncLatestDrawTriggersServiceWithScheduledSourceWhenEnabled() {
        LotteryDrawSyncService syncService = org.mockito.Mockito.mock(LotteryDrawSyncService.class);
        SyncProperties syncProperties = enabledSyncProperties();
        LotteryAutoSyncScheduler scheduler = new LotteryAutoSyncScheduler(
                syncService,
                syncProperties);

        scheduler.syncLatestDraw();

        verify(syncService).syncLatestDraw("SCHEDULED");
    }

    /**
     * 验证已有活跃任务或上游失败时只记录日志，不让异常打断调度线程。
     */
    @Test
    void syncLatestDrawSwallowsServiceExceptionWhenEnabled() {
        LotteryDrawSyncService syncService = org.mockito.Mockito.mock(LotteryDrawSyncService.class);
        SyncProperties syncProperties = enabledSyncProperties();
        LotteryAutoSyncScheduler scheduler = new LotteryAutoSyncScheduler(
                syncService,
                syncProperties);
        when(syncService.syncLatestDraw("SCHEDULED"))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST, "当前已有同步任务正在执行"));

        scheduler.syncLatestDraw();

        verify(syncService).syncLatestDraw("SCHEDULED");
    }

    private SyncProperties enabledSyncProperties() {
        SyncProperties properties = new SyncProperties();
        properties.autoLatest().setEnabled(true);
        return properties;
    }
}
