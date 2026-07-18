package com.hotchpotch.lottery.favorite.enums;

/**
 * 用户收藏号码状态。
 */
public enum LotteryNumberFavoriteStatus {

    /** 有效收藏。 */
    ACTIVE,

    /** 已取消收藏。 */
    CANCELLED;

    /**
     * 返回数据库存储使用的收藏状态编码。
     */
    public String code() {
        return name();
    }
}
