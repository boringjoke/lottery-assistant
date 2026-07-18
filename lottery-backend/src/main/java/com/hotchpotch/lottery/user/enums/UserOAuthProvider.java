package com.hotchpotch.lottery.user.enums;

/**
 * 用户第三方登录平台。
 */
public enum UserOAuthProvider {

    /** 微信公众号。 */
    WECHAT_MP,

    /** 微信网页扫码登录。 */
    WECHAT_WEB,

    /** 微信小程序。 */
    WECHAT_MINI_PROGRAM;

    /**
     * 返回数据库存储使用的第三方登录平台编码。
     */
    public String code() {
        return name();
    }
}
