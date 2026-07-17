package com.hotchpotch.lottery.draw.record;

import java.time.LocalDate;

/**
 * 按开奖日期范围异步同步请求参数。
 *
 * @param startDate 起始开奖日期，较早日期
 * @param endDate 结束开奖日期，较晚日期
 * @param startPage 历史同步起始页码
 * @param pageSize 每页数量
 * @param pageDelayMillis 每页同步间隔毫秒数
 * @param stopWhenLastPage 遇到最后一页时是否停止
 */
public record LotteryDateRangeSyncRequest(
        LocalDate startDate,
        LocalDate endDate,
        Integer startPage,
        Integer pageSize,
        Integer pageDelayMillis,
        Boolean stopWhenLastPage) {
}
