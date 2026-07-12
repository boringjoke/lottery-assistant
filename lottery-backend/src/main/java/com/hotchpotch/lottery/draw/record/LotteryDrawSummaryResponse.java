package com.hotchpotch.lottery.draw.record;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 开奖历史列表摘要响应数据。
 */
public record LotteryDrawSummaryResponse(
        String lotteryType,
        String issueNo,
        LocalDate drawDate,
        String frontNumbers,
        String backNumbers,
        BigDecimal poolBalance,
        BigDecimal salesAmount) {
}
