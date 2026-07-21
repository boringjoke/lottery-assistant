package com.hotchpotch.lottery.notification.service;

import com.hotchpotch.lottery.common.constant.PageConstants;
import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.notification.entity.UserNotification;
import com.hotchpotch.lottery.notification.enums.NotificationBusinessType;
import com.hotchpotch.lottery.notification.enums.NotificationReadStatus;
import com.hotchpotch.lottery.notification.enums.NotificationType;
import com.hotchpotch.lottery.notification.record.UserNotificationPageResponse;
import com.hotchpotch.lottery.notification.record.UserNotificationResponse;
import com.hotchpotch.lottery.notification.repository.UserNotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户站内通知服务。
 */
@Service
public class UserNotificationService {

    private static final int BUSINESS_KEY_MAX_LENGTH = 128;
    private static final int TITLE_MAX_LENGTH = 128;
    private static final int CONTENT_MAX_LENGTH = 512;

    private final UserNotificationRepository notificationRepository;

    public UserNotificationService(UserNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * 幂等创建站内通知。
     */
    @Transactional
    public UserNotificationResponse createNotification(
            Long userId,
            NotificationType notificationType,
            NotificationBusinessType businessType,
            String businessKey,
            String title,
            String content) {
        requireUserId(userId);
        String normalizedNotificationType = requireEnumCode(notificationType, "通知类型不能为空");
        String normalizedBusinessType = requireEnumCode(businessType, "业务类型不能为空");
        String normalizedBusinessKey = requireText(businessKey, "业务幂等键不能为空", BUSINESS_KEY_MAX_LENGTH);
        String normalizedTitle = requireText(title, "通知标题不能为空", TITLE_MAX_LENGTH);
        String normalizedContent = requireText(content, "通知内容不能为空", CONTENT_MAX_LENGTH);

        UserNotification existingNotification = notificationRepository
                .findByNotificationTypeAndBusinessKey(normalizedNotificationType, normalizedBusinessKey)
                .orElse(null);
        if (existingNotification != null) {
            return toResponse(existingNotification);
        }

        return insertNotification(
                userId,
                normalizedNotificationType,
                normalizedBusinessType,
                normalizedBusinessKey,
                normalizedTitle,
                normalizedContent);
    }

    /**
     * 分页查询当前用户站内通知。
     */
    public UserNotificationPageResponse listNotifications(Long userId, int pageNo, int pageSize) {
        requireUserId(userId);
        int safePageNo = Math.max(pageNo, PageConstants.DEFAULT_PAGE_NO);
        int safePageSize = Math.min(Math.max(pageSize, 1), PageConstants.MAX_PAGE_SIZE);
        long total = notificationRepository.countByUserId(userId);
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / safePageSize);
        List<UserNotificationResponse> notifications = notificationRepository
                .findPageByUserId(userId, safePageNo, safePageSize)
                .stream()
                .map(this::toResponse)
                .toList();

        return new UserNotificationPageResponse(
                safePageNo,
                safePageSize,
                total,
                pages,
                notifications);
    }

    /**
     * 查询当前用户未读通知数量。
     */
    public long unreadCount(Long userId) {
        requireUserId(userId);

        return notificationRepository.countByUserIdAndReadStatus(
                userId,
                NotificationReadStatus.UNREAD.code());
    }

    /**
     * 将当前用户一条通知标记为已读。
     */
    @Transactional
    public UserNotificationResponse markAsRead(Long userId, Long notificationId) {
        UserNotification notification = findOwnedNotification(userId, notificationId);
        if (NotificationReadStatus.READ.code().equals(notification.getReadStatus())) {
            return toResponse(notification);
        }

        LocalDateTime now = LocalDateTime.now();
        notification.setReadStatus(NotificationReadStatus.READ.code());
        notification.setReadTime(now);
        notification.setUpdateTime(now);
        notificationRepository.updateById(notification);

        return toResponse(notification);
    }

    /**
     * 将当前用户全部未读通知标记为已读。
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        requireUserId(userId);
        List<UserNotification> unreadNotifications = notificationRepository.findUnreadByUserId(
                userId,
                NotificationReadStatus.UNREAD.code());
        LocalDateTime now = LocalDateTime.now();

        for (UserNotification notification : unreadNotifications) {
            notification.setReadStatus(NotificationReadStatus.READ.code());
            notification.setReadTime(now);
            notification.setUpdateTime(now);
            notificationRepository.updateById(notification);
        }

        return unreadNotifications.size();
    }

    private UserNotificationResponse insertNotification(
            Long userId,
            String notificationType,
            String businessType,
            String businessKey,
            String title,
            String content) {
        LocalDateTime now = LocalDateTime.now();
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setNotificationType(notificationType);
        notification.setBusinessType(businessType);
        notification.setBusinessKey(businessKey);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReadStatus(NotificationReadStatus.UNREAD.code());
        notification.setReadTime(null);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        notificationRepository.insert(notification);

        return toResponse(notification);
    }

    private UserNotification findOwnedNotification(Long userId, Long notificationId) {
        requireUserId(userId);
        if (notificationId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "通知 ID 不能为空");
        }

        UserNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(this::notificationNotFound);
        if (!userId.equals(notification.getUserId())) {
            throw notificationNotFound();
        }

        return notification;
    }

    private void requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String requireEnumCode(Enum<?> value, String errorMessage) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, errorMessage);
        }

        return value.name();
    }

    private String requireText(String value, String errorMessage, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, errorMessage);
        }

        String normalizedValue = value.trim();
        if (normalizedValue.length() > maxLength) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, errorMessage.replace("不能为空", "不能超过 " + maxLength + " 个字符"));
        }

        return normalizedValue;
    }

    private UserNotificationResponse toResponse(UserNotification notification) {
        return new UserNotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getNotificationType(),
                notification.getBusinessType(),
                notification.getBusinessKey(),
                notification.getTitle(),
                notification.getContent(),
                notification.getReadStatus(),
                notification.getReadTime(),
                notification.getCreateTime(),
                notification.getUpdateTime());
    }

    private BusinessException notificationNotFound() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "站内通知不存在");
    }
}
