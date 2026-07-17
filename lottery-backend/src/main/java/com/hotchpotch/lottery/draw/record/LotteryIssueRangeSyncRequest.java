package com.hotchpotch.lottery.draw.record;

/**
 * 按期号范围异步同步请求参数。
 *
 * @param startIssueNo 起始期号，较小期号
 * @param endIssueNo 结束期号，较大期号
 * @param startPage 历史同步起始页码
 * @param pageSize 每页数量
 * @param pageDelayMillis 每页同步间隔毫秒数
 * @param stopWhenLastPage 遇到最后一页时是否停止
 */
public record LotteryIssueRangeSyncRequest(
        String startIssueNo,
        String endIssueNo,
        Integer startPage,
        Integer pageSize,
        Integer pageDelayMillis,
        Boolean stopWhenLastPage) {
}
