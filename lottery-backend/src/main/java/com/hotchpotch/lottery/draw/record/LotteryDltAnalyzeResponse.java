package com.hotchpotch.lottery.draw.record;

import java.util.List;

/**
 * 大乐透号码分析响应。
 *
 * @param totalNumberCount 分析号码总注数
 * @param winningNumberCount 存在中奖记录的注数
 * @param winningHitCount 中奖记录总次数
 * @param bestPrizeLevel 全部号码中的最高奖级序号，未中奖时为空
 * @param bestPrizeName 全部号码中的最高奖级名称
 * @param results 每注号码分析结果
 */
public record LotteryDltAnalyzeResponse(
        int totalNumberCount,
        int winningNumberCount,
        int winningHitCount,
        Integer bestPrizeLevel,
        String bestPrizeName,
        List<LotteryDltAnalyzeNumberResult> results) {
}
