package com.hotchpotch.lottery.draw.record;

import java.util.List;

/**
 * 开奖历史分页响应数据。
 */
public record LotteryDrawPageResponse(
        Integer pageNo,
        Integer pageSize,
        Long total,
        Integer pages,
        List<LotteryDrawSummaryResponse> draws) {
}
