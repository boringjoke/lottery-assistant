package com.hotchpotch.lottery.crawler.record;

import java.math.BigDecimal;

/**
 * crawler 返回的奖级明细。
 */
public record CrawlerPrizeTierResponse(
        String name,
        Integer stakeCount,
        BigDecimal stakeAmount,
        BigDecimal totalPrizeAmount,
        Integer sort,
        String group) {
}
