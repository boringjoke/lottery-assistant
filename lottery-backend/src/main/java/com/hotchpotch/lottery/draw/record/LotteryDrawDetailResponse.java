package com.hotchpotch.lottery.draw.record;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 开奖详情响应数据。
 *
 * @param lotteryType 彩票类型编码
 * @param issueNo 开奖期号
 * @param drawDate 开奖日期
 * @param frontNumbers 前区号码，使用逗号分隔
 * @param backNumbers 后区号码，使用逗号分隔
 * @param poolBalance 奖池余额
 * @param salesAmount 销售金额
 * @param sourceUrl 数据来源地址
 * @param pdfUrl 开奖公告 PDF 地址
 * @param prizeTiers 奖级明细列表
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
