package com.hotchpotch.lottery.user.enums;

/**
 * 用户状态。
 */
public enum UserStatus {

    /** 正常可用。 */
    ACTIVE,

    /** 已禁用。 */
    DISABLED;

    /**
     * 返回数据库存储使用的用户状态编码。
     */
    public String code() {
        return name();
    }
}
