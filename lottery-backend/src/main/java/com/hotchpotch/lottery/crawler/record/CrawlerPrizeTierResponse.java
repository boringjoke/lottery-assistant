package com.hotchpotch.lottery.crawler.record;

import java.math.BigDecimal;

/**
 * crawler 返回的奖级明细。
 *
 * @param name 奖级名称
 * @param stakeCount 中奖注数
 * @param stakeAmount 单注奖金金额
 * @param totalPrizeAmount 当前奖级总奖金金额
 * @param sort 排序序号
 * @param group 奖级分组
 */
public record CrawlerPrizeTierResponse(
        String name,
        Integer stakeCount,
        BigDecimal stakeAmount,
        BigDecimal totalPrizeAmount,
        Integer sort,
        String group) {
}
