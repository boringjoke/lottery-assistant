package com.hotchpotch.lottery.crawler.record;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * crawler 返回的单期开奖数据。
 *
 * @param lotteryType 彩票类型编码
 * @param issueNo 开奖期号
 * @param drawDate 开奖日期
 * @param frontNumbers 前区号码
 * @param backNumbers 后区号码
 * @param poolBalance 奖池余额
 * @param salesAmount 销售金额
 * @param prizeTiers 奖级明细
 * @param source 数据来源地址
 * @param pdfUrl 开奖公告 PDF 地址
 */
public record CrawlerDraw(
        String lotteryType,
        String issueNo,
        LocalDate drawDate,
        List<Integer> frontNumbers,
        List<Integer> backNumbers,
        BigDecimal poolBalance,
        BigDecimal salesAmount,
        List<CrawlerPrizeTierResponse> prizeTiers,
        String source,
        String pdfUrl) {
}
