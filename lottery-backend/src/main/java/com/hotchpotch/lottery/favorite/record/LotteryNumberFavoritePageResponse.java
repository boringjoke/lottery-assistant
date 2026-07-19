package com.hotchpotch.lottery.favorite.record;

import java.util.List;

/**
 * 收藏号码分页响应。
 *
 * @param pageNo 当前页码
 * @param pageSize 每页数量
 * @param total 总记录数
 * @param pages 总页数
 * @param status 状态筛选
 * @param keyword 关键字筛选
 * @param favorites 收藏号码列表
 */
public record LotteryNumberFavoritePageResponse(
        Integer pageNo,
        Integer pageSize,
        Long total,
        Integer pages,
        String status,
        String keyword,
        List<LotteryNumberFavoriteResponse> favorites) {
}
