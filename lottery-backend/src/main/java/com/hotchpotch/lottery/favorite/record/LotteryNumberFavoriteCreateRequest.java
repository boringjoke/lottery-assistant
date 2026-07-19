package com.hotchpotch.lottery.favorite.record;

import java.util.List;

/**
 * 新增收藏号码请求。
 *
 * @param lotteryType 彩票类型编码，MVP 仅支持 DLT
 * @param frontNumbers 前区号码
 * @param backNumbers 后区号码
 * @param favoriteName 收藏名称
 * @param remark 备注
 */
public record LotteryNumberFavoriteCreateRequest(
        String lotteryType,
        List<Integer> frontNumbers,
        List<Integer> backNumbers,
        String favoriteName,
        String remark) {
}
