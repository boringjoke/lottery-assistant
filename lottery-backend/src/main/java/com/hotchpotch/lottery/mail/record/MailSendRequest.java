package com.hotchpotch.lottery.mail.record;

/**
 * 邮件发送请求。
 *
 * @param userId 用户 ID，可为空
 * @param businessType 业务类型，可为空
 * @param businessKey 业务键，可为空
 * @param toEmail 收件邮箱
 * @param subject 邮件标题
 * @param content 邮件正文
 */
public record MailSendRequest(
        Long userId,
        String businessType,
        String businessKey,
        String toEmail,
        String subject,
        String content) {
}
