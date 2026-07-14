package com.hotchpotch.lottery.draw.record;

/**
 * 历史开奖异步同步请求参数。
 *
 * @param startPage 历史同步起始页码
 * @param pageSize 每页数量
 * @param maxPages 最大同步页数
 * @param pageDelayMillis 每页同步间隔毫秒数
 * @param stopWhenLastPage 遇到最后一页时是否停止
 */
public record LotteryHistorySyncRequest(
        Integer startPage,
        Integer pageSize,
        Integer maxPages,
        Integer pageDelayMillis,
        Boolean stopWhenLastPage) {
}
