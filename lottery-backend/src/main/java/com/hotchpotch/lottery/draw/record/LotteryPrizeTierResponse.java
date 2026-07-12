package com.hotchpotch.lottery.draw.record;

import java.math.BigDecimal;

/**
 * 开奖奖级明细响应数据。
 */
public record LotteryPrizeTierResponse(
        String prizeName,
        Integer stakeCount,
        BigDecimal stakeAmount,
        BigDecimal totalPrizeAmount,
        Integer sortOrder,
        String prizeGroup) {
}
