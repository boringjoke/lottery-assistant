package com.hotchpotch.lottery.favorite.record;

import java.time.LocalDate;
import java.util.List;

/**
 * 收藏号码中奖历史摘要响应。
 *
 * @param winning 是否曾经中奖
 * @param totalWinningCount 累计中奖次数
 * @param bestPrizeLevel 历史最高奖级序号
 * @param bestPrizeName 历史最高奖级名称
 * @param bestIssueNo 历史最高奖级对应期号
 * @param bestDrawDate 历史最高奖级对应开奖日期
 * @param prizeCounts 各奖级命中次数
 */
public record LotteryFavoriteWinningSummaryResponse(
        boolean winning,
        long totalWinningCount,
        Integer bestPrizeLevel,
        String bestPrizeName,
        String bestIssueNo,
        LocalDate bestDrawDate,
        List<LotteryFavoritePrizeCountResponse> prizeCounts) {
}
