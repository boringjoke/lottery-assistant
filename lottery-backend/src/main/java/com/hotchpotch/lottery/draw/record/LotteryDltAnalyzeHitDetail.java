package com.hotchpotch.lottery.draw.record;

import java.time.LocalDate;

/**
 * 大乐透号码单期开奖命中明细。
 *
 * @param issueNo 开奖期号
 * @param drawDate 开奖日期
 * @param drawFrontNumbers 开奖前区号码
 * @param drawBackNumbers 开奖后区号码
 * @param frontHitCount 前区命中数量
 * @param backHitCount 后区命中数量
 * @param winning 是否中奖
 * @param prizeLevel 奖级序号，未中奖时为空
 * @param prizeName 奖级名称
 * @param ruleVersion 奖级规则版本
 */
public record LotteryDltAnalyzeHitDetail(
        String issueNo,
        LocalDate drawDate,
        String drawFrontNumbers,
        String drawBackNumbers,
        int frontHitCount,
        int backHitCount,
        boolean winning,
        Integer prizeLevel,
        String prizeName,
        String ruleVersion) {
}
