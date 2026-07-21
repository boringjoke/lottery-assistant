package com.hotchpotch.lottery.mail.record;

/**
 * 管理端测试邮件发送请求。
 *
 * @param toEmail 收件邮箱
 * @param subject 邮件标题
 * @param content 邮件正文
 */
public record AdminMailSendTestRequest(
        String toEmail,
        String subject,
        String content) {
}
