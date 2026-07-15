package com.hotchpotch.lottery.draw.record;

import java.util.List;

/**
 * 大乐透单注号码分析结果。
 *
 * @param lineNo 输入行号
 * @param inputText 原始输入文本
 * @param displayText 规范化展示文本
 * @param frontNumbers 规范化前区号码
 * @param backNumbers 规范化后区号码
 * @param winning 是否存在中奖记录
 * @param winningHitCount 中奖记录数量
 * @param bestPrizeLevel 最高奖级序号，未中奖时为空
 * @param bestPrizeName 最高奖级名称
 * @param hitDetails 中奖命中明细
 */
public record LotteryDltAnalyzeNumberResult(
        int lineNo,
        String inputText,
        String displayText,
        List<Integer> frontNumbers,
        List<Integer> backNumbers,
        boolean winning,
        int winningHitCount,
        Integer bestPrizeLevel,
        String bestPrizeName,
        List<LotteryDltAnalyzeHitDetail> hitDetails) {
}
