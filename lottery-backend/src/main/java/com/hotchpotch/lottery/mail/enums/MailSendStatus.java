package com.hotchpotch.lottery.mail.enums;

/**
 * 邮件发送状态。
 */
public enum MailSendStatus {

    /** 等待发送。 */
    PENDING,

    /** 发送成功。 */
    SUCCESS,

    /** 发送失败。 */
    FAILED;

    /**
     * 返回数据库存储使用的状态编码。
     */
    public String code() {
        return name();
    }
}
