package com.hotchpotch.lottery.crawler.record;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * crawler 返回的单期开奖数据。
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
