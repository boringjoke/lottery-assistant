package com.hotchpotch.lottery.draw.record;

/**
 * 历史开奖异步同步请求参数。
 */
public record LotteryHistorySyncRequest(
        Integer startPage,
        Integer pageSize,
        Integer maxPages,
        Integer pageDelayMillis,
        Boolean stopWhenLastPage) {
}
