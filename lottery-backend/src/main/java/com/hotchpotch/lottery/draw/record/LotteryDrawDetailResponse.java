package com.hotchpotch.lottery.draw.record;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 开奖详情响应数据。
 */
public record LotteryDrawDetailResponse(
        String lotteryType,
        String issueNo,
        LocalDate drawDate,
        String frontNumbers,
        String backNumbers,
        BigDecimal poolBalance,
        BigDecimal salesAmount,
        String sourceUrl,
        String pdfUrl,
        List<LotteryPrizeTierResponse> prizeTiers) {
}
