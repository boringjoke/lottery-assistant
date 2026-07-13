package com.hotchpotch.lottery.draw.record;

import java.time.LocalDateTime;

/**
 * 开奖同步任务进度响应。
 */
public record LotterySyncTaskResponse(
        String taskNo,
        String lotteryType,
        String syncType,
        String triggerSource,
        String status,
        Integer startPage,
        Integer currentPage,
        Integer lastSuccessPage,
        Integer failedPage,
        Integer pageSize,
        Integer maxPages,
        Integer pageDelayMillis,
        Boolean stopWhenLastPage,
        Integer successCount,
        Integer skippedCount,
        Integer failedCount,
        String failureReason,
        LocalDateTime startTime,
        LocalDateTime finishTime) {
}
