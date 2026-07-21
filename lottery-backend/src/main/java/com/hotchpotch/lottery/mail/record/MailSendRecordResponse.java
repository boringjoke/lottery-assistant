package com.hotchpotch.lottery.mail.record;

import java.time.LocalDateTime;

/**
 * 邮件发送记录响应。
 *
 * @param id 记录 ID
 * @param userId 用户 ID
 * @param businessType 业务类型
 * @param businessKey 业务键
 * @param fromEmail 发件邮箱
 * @param toEmail 收件邮箱
 * @param subject 邮件标题
 * @param sendStatus 发送状态
 * @param errorMessage 失败原因
 * @param attemptCount 发送尝试次数
 * @param sentTime 发送成功时间
 * @param createTime 创建时间
 * @param updateTime 更新时间
 */
public record MailSendRecordResponse(
        Long id,
        Long userId,
        String businessType,
        String businessKey,
        String fromEmail,
        String toEmail,
        String subject,
        String sendStatus,
        String errorMessage,
        Integer attemptCount,
        LocalDateTime sentTime,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
