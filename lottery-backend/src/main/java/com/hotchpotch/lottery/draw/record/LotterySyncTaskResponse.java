package com.hotchpotch.lottery.draw.record;

import java.time.LocalDateTime;

/**
 * 开奖同步任务进度响应。
 *
 * @param taskNo 同步任务编号
 * @param lotteryType 彩票类型编码
 * @param syncType 同步类型
 * @param triggerSource 触发来源
 * @param status 同步任务状态
 * @param startPage 历史同步起始页码
 * @param currentPage 当前同步页码
 * @param lastSuccessPage 最后成功同步页码
 * @param failedPage 失败页码
 * @param pageSize 每页数量
 * @param maxPages 最大同步页数
 * @param pageDelayMillis 每页同步间隔毫秒数
 * @param stopWhenLastPage 遇到最后一页时是否停止
 * @param successCount 成功同步数量
 * @param skippedCount 跳过数量
 * @param failedCount 失败数量
 * @param failureReason 失败原因摘要
 * @param startTime 开始时间
 * @param finishTime 结束时间
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
