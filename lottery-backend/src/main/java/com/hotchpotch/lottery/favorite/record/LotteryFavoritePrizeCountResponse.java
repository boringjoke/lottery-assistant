package com.hotchpotch.lottery.favorite.record;

/**
 * 收藏号码命中奖级统计响应。
 *
 * @param prizeLevel 奖级序号
 * @param prizeName 奖级名称
 * @param count 命中次数
 */
public record LotteryFavoritePrizeCountResponse(
        Integer prizeLevel,
        String prizeName,
        long count) {
}
