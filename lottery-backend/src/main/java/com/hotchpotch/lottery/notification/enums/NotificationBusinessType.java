package com.hotchpotch.lottery.notification.enums;

/**
 * 站内通知关联业务类型。
 */
public enum NotificationBusinessType {

    /** 彩票收藏中奖业务。 */
    LOTTERY_FAVORITE_WINNING;

    /**
     * 返回数据库存储使用的业务类型编码。
     */
    public String code() {
        return name();
    }
}
