package com.hotchpotch.lottery.notification.record;

import java.time.LocalDateTime;

/**
 * 用户站内通知响应。
 *
 * @param id 通知 ID
 * @param userId 用户 ID
 * @param notificationType 通知类型
 * @param businessType 业务类型
 * @param businessKey 业务幂等键
 * @param title 通知标题
 * @param content 通知内容
 * @param readStatus 阅读状态
 * @param readTime 阅读时间
 * @param createTime 创建时间
 * @param updateTime 更新时间
 */
public record UserNotificationResponse(
        Long id,
        Long userId,
        String notificationType,
        String businessType,
        String businessKey,
        String title,
        String content,
        String readStatus,
        LocalDateTime readTime,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
