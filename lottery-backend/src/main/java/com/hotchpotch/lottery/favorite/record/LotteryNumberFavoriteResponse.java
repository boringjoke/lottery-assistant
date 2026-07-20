package com.hotchpotch.lottery.favorite.record;

import java.time.LocalDateTime;

/**
 * 收藏号码响应。
 *
 * @param id 收藏 ID
 * @param lotteryType 彩票类型编码
 * @param frontNumbers 前区号码，逗号分隔
 * @param backNumbers 后区号码，逗号分隔
 * @param displayText 展示用号码文本
 * @param favoriteName 收藏名称
 * @param remark 备注
 * @param status 收藏状态
 * @param favoriteTime 首次收藏时间
 * @param effectiveTime 当前生效时间
 * @param cancelTime 取消收藏时间
 * @param latestDrawResult 最近一期开奖实时分析结果
 */
public record LotteryNumberFavoriteResponse(
        Long id,
        String lotteryType,
        String frontNumbers,
        String backNumbers,
        String displayText,
        String favoriteName,
        String remark,
        String status,
        LocalDateTime favoriteTime,
        LocalDateTime effectiveTime,
        LocalDateTime cancelTime,
        LotteryFavoriteDrawHistoryItemResponse latestDrawResult) {
}
