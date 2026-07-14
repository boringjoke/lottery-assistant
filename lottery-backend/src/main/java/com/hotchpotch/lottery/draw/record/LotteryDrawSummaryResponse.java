package com.hotchpotch.lottery.draw.record;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 开奖历史列表摘要响应数据。
 *
 * @param lotteryType 彩票类型编码
 * @param issueNo 开奖期号
 * @param drawDate 开奖日期
 * @param frontNumbers 前区号码，使用逗号分隔
 * @param backNumbers 后区号码，使用逗号分隔
 * @param poolBalance 奖池余额
 * @param salesAmount 销售金额
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
