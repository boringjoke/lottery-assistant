package com.hotchpotch.lottery.draw.record;

import java.math.BigDecimal;

/**
 * 开奖奖级明细响应数据。
 *
 * @param prizeName 奖级名称
 * @param stakeCount 中奖注数
 * @param stakeAmount 单注奖金金额
 * @param totalPrizeAmount 当前奖级总奖金金额
 * @param sortOrder 排序序号
 * @param prizeGroup 奖级分组
 */
public record LotteryPrizeTierResponse(
        String prizeName,
        Integer stakeCount,
        BigDecimal stakeAmount,
        BigDecimal totalPrizeAmount,
        Integer sortOrder,
        String prizeGroup) {
}
