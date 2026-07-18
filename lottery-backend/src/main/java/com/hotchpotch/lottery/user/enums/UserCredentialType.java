package com.hotchpotch.lottery.user.enums;

/**
 * 用户登录凭证类型。
 */
public enum UserCredentialType {

    /** 用户名。 */
    USERNAME,

    /** 手机号。 */
    PHONE,

    /** 邮箱。 */
    EMAIL;

    /**
     * 返回数据库存储使用的登录凭证类型编码。
     */
    public String code() {
        return name();
    }
}
