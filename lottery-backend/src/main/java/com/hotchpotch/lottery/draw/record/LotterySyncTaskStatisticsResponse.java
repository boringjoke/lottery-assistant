package com.hotchpotch.lottery.draw.record;

import java.time.LocalDateTime;

/**
 * 同步任务状态统计响应，用于管理页顶部概览。
 *
 * @param runningCount 运行中任务数量
 * @param pendingCount 待执行任务数量
 * @param failedCount 失败任务数量
 * @param successCountToday 今日成功任务数量
 * @param latestSuccessTime 最近一次成功任务结束时间
 * @param latestFailureTime 最近一次失败任务结束时间
 * @param latestFailureMessage 最近一次失败原因摘要
 */
public record LotterySyncTaskStatisticsResponse(
        Long runningCount,
        Long pendingCount,
        Long failedCount,
        Long successCountToday,
        LocalDateTime latestSuccessTime,
        LocalDateTime latestFailureTime,
        String latestFailureMessage) {
}
