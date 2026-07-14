package com.hotchpotch.lottery.draw.record;

/**
 * 单次开奖同步结果。
 *
 * @param taskNo 同步任务编号
 * @param lotteryType 彩票类型编码
 * @param issueNo 开奖期号
 * @param status 同步任务状态
 * @param successCount 成功同步数量
 * @param skippedCount 跳过数量
 * @param failedCount 失败数量
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
