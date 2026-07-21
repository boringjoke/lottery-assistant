package com.hotchpotch.lottery.notification.enums;

/**
 * 站内通知阅读状态。
 */
public enum NotificationReadStatus {

    /** 未读。 */
    UNREAD,

    /** 已读。 */
    READ;

    /**
     * 返回数据库存储使用的阅读状态编码。
     */
    public String code() {
        return name();
    }
}
