package com.hotchpotch.lottery.notification.record;

import java.util.List;

/**
 * 用户站内通知分页响应。
 *
 * @param pageNo 当前页码
 * @param pageSize 每页数量
 * @param total 总记录数
 * @param pages 总页数
 * @param notifications 通知列表
 */
public record UserNotificationPageResponse(
        int pageNo,
        int pageSize,
        long total,
        int pages,
        List<UserNotificationResponse> notifications) {
}
