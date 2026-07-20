package com.hotchpotch.lottery.draw.component;

import com.hotchpotch.lottery.config.SyncProperties;
import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.draw.enums.LotterySyncTriggerSource;
import com.hotchpotch.lottery.draw.service.LotteryDrawSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 最新开奖自动同步定时任务。
 */
@Component
public class LotteryAutoSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(LotteryAutoSyncScheduler.class);

    private final LotteryDrawSyncService syncService;
    private final SyncProperties syncProperties;

    public LotteryAutoSyncScheduler(
            LotteryDrawSyncService syncService,
            SyncProperties syncProperties) {
        this.syncService = syncService;
        this.syncProperties = syncProperties;
    }

    /**
     * 按配置的 cron 尝试同步最新开奖；默认关闭，避免本地启动后自动请求 crawler。
     */
    @Scheduled(cron = "${lottery.sync.auto-latest.cron}", zone = "${lottery.sync.auto-latest.zone}")
    public void syncLatestDraw() {
        if (!syncProperties.autoLatest().enabled()) {
            return;
        }

        try {
            syncService.syncLatestDraw(LotterySyncTriggerSource.SCHEDULED.code());
        } catch (BusinessException ex) {
            if (isActiveTaskConflict(ex)) {
                log.info("自动同步最新开奖跳过: {}", ex.getMessage());
                return;
            }
            log.warn("自动同步最新开奖执行失败: {}", ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            log.warn("自动同步最新开奖执行失败: {}", ex.getMessage(), ex);
        }
    }

    /**
     * 已有活跃同步任务时不重复创建自动任务，交给下一次 cron 再尝试。
     */
    private boolean isActiveTaskConflict(BusinessException ex) {
        return ex.getMessage() != null && ex.getMessage().contains("当前已有同步任务正在执行");
    }
}
