package com.hotchpotch.lottery.favorite.record;

/**
 * 修改收藏号码请求。
 *
 * @param favoriteId 收藏 ID
 * @param favoriteName 收藏名称
 * @param remark 备注
 */
public record LotteryNumberFavoriteUpdateRequest(
        Long favoriteId,
        String favoriteName,
        String remark) {
}
