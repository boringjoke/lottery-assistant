package com.hotchpotch.lottery.notification.enums;

/**
 * 站内通知类型。
 */
public enum NotificationType {

    /** 收藏号码中奖提醒。 */
    FAVORITE_WINNING;

    /**
     * 返回数据库存储使用的通知类型编码。
     */
    public String code() {
        return name();
    }
}
