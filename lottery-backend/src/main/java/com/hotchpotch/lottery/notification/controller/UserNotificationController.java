package com.hotchpotch.lottery.notification.controller;

import com.hotchpotch.lottery.common.constant.PageConstants;
import com.hotchpotch.lottery.common.response.ApiResponse;
import com.hotchpotch.lottery.notification.record.UserNotificationPageResponse;
import com.hotchpotch.lottery.notification.record.UserNotificationResponse;
import com.hotchpotch.lottery.notification.service.UserNotificationService;
import com.hotchpotch.lottery.user.security.CurrentUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户站内通知接口。
 */
@RestController
@RequestMapping("/api/user/notifications")
public class UserNotificationController {

    private final UserNotificationService notificationService;
    private final CurrentUserContext currentUserContext;

    public UserNotificationController(
            UserNotificationService notificationService,
            CurrentUserContext currentUserContext) {
        this.notificationService = notificationService;
        this.currentUserContext = currentUserContext;
    }

    /**
     * 分页查询当前用户站内通知。
     */
    @GetMapping("/page")
    public ApiResponse<UserNotificationPageResponse> listNotifications(
            @RequestParam(defaultValue = PageConstants.DEFAULT_PAGE_NO_TEXT) int pageNo,
            @RequestParam(defaultValue = PageConstants.DEFAULT_PAGE_SIZE_TEXT) int pageSize) {
        return ApiResponse.success(notificationService.listNotifications(
                currentUserContext.requireUserId(),
                pageNo,
                pageSize));
    }

    /**
     * 查询当前用户未读通知数量。
     */
    @GetMapping("/unreadCount")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.success(notificationService.unreadCount(currentUserContext.requireUserId()));
    }

    /**
     * 将当前用户一条通知标记为已读。
     */
    @PostMapping("/{notificationId}/read")
    public ApiResponse<UserNotificationResponse> markAsRead(
            @PathVariable Long notificationId) {
        return ApiResponse.success(notificationService.markAsRead(
                currentUserContext.requireUserId(),
                notificationId));
    }

    /**
     * 将当前用户全部未读通知标记为已读。
     */
    @PostMapping("/readAll")
    public ApiResponse<Integer> markAllAsRead() {
        return ApiResponse.success(notificationService.markAllAsRead(currentUserContext.requireUserId()));
    }
}
