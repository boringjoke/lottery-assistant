package com.hotchpotch.lottery.draw.record;

/**
 * 大乐透奖级判定结果。
 *
 * @param frontHitCount 前区命中数量
 * @param backHitCount 后区命中数量
 * @param winning 是否中奖
 * @param prizeLevel 奖级序号，未中奖时为空
 * @param prizeName 奖级名称，未中奖时为“未中奖”
 * @param ruleVersion 奖级规则版本
 */
public record LotteryDltPrizeResult(
        int frontHitCount,
        int backHitCount,
        boolean winning,
        Integer prizeLevel,
        String prizeName,
        String ruleVersion) {
}
