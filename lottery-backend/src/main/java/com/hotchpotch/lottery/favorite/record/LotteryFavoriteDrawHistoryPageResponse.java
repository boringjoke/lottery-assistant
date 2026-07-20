package com.hotchpotch.lottery.favorite.record;

import java.util.List;

/**
 * 收藏号码开奖历史实时分析分页响应。
 *
 * @param pageNo 当前页码
 * @param pageSize 每页数量
 * @param total 总记录数
 * @param pages 总页数
 * @param favoriteId 收藏 ID
 * @param lotteryType 彩票类型编码
 * @param frontNumbers 收藏前区号码
 * @param backNumbers 收藏后区号码
 * @param displayText 收藏号码展示文本
 * @param latestDrawResult 最近一期实时分析结果
 * @param results 当前页实时分析结果
 */
public record LotteryFavoriteDrawHistoryPageResponse(
        int pageNo,
        int pageSize,
        long total,
        int pages,
        Long favoriteId,
        String lotteryType,
        String frontNumbers,
        String backNumbers,
        String displayText,
        LotteryFavoriteDrawHistoryItemResponse latestDrawResult,
        List<LotteryFavoriteDrawHistoryItemResponse> results) {
}
