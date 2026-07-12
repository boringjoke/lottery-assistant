package com.hotchpotch.lottery.draw.record;

/**
 * 单次开奖同步结果。
 */
public record LotteryDrawSyncResult(
        String taskNo,
        String lotteryType,
        String issueNo,
        String status,
        int successCount,
        int skippedCount,
        int failedCount) {
}
