package com.hotchpotch.lottery.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.notification.entity.UserNotification;
import com.hotchpotch.lottery.notification.enums.NotificationBusinessType;
import com.hotchpotch.lottery.notification.enums.NotificationReadStatus;
import com.hotchpotch.lottery.notification.enums.NotificationType;
import com.hotchpotch.lottery.notification.repository.UserNotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserNotificationServiceTest {

    @Mock
    private UserNotificationRepository notificationRepository;

    /**
     * 验证创建站内通知会规范化文本并默认未读。
     */
    @Test
    void createNotificationInsertsUnreadNotification() {
        when(notificationRepository.findByNotificationTypeAndBusinessKey(
                "FAVORITE_WINNING",
                "DLT:26076:FAVORITE:101"))
                .thenReturn(Optional.empty());
        when(notificationRepository.insert(any())).thenAnswer(invocation -> {
            UserNotification notification = invocation.getArgument(0);
            notification.setId(88L);
            return 1;
        });
        UserNotificationService service = new UserNotificationService(notificationRepository);

        var response = service.createNotification(
                10L,
                NotificationType.FAVORITE_WINNING,
                NotificationBusinessType.LOTTERY_FAVORITE_WINNING,
                " DLT:26076:FAVORITE:101 ",
                " 收藏号码中奖提醒 ",
                " 你收藏的号码中奖啦 ");

        ArgumentCaptor<UserNotification> notificationCaptor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepository).insert(notificationCaptor.capture());
        UserNotification inserted = notificationCaptor.getValue();
        assertThat(inserted.getUserId()).isEqualTo(10L);
        assertThat(inserted.getNotificationType()).isEqualTo("FAVORITE_WINNING");
        assertThat(inserted.getBusinessType()).isEqualTo("LOTTERY_FAVORITE_WINNING");
        assertThat(inserted.getBusinessKey()).isEqualTo("DLT:26076:FAVORITE:101");
        assertThat(inserted.getTitle()).isEqualTo("收藏号码中奖提醒");
        assertThat(inserted.getContent()).isEqualTo("你收藏的号码中奖啦");
        assertThat(inserted.getReadStatus()).isEqualTo("UNREAD");
        assertThat(inserted.getCreateTime()).isNotNull();
        assertThat(response.id()).isEqualTo(88L);
        assertThat(response.readStatus()).isEqualTo("UNREAD");
    }

    /**
     * 验证相同通知类型和业务键已存在时不重复插入。
     */
    @Test
    void createNotificationReturnsExistingNotificationByBusinessKey() {
        UserNotification existing = notification(88L, 10L, "UNREAD");
        when(notificationRepository.findByNotificationTypeAndBusinessKey(
                "FAVORITE_WINNING",
                "DLT:26076:FAVORITE:101"))
                .thenReturn(Optional.of(existing));
        UserNotificationService service = new UserNotificationService(notificationRepository);

        var response = service.createNotification(
                10L,
                NotificationType.FAVORITE_WINNING,
                NotificationBusinessType.LOTTERY_FAVORITE_WINNING,
                "DLT:26076:FAVORITE:101",
                "收藏号码中奖提醒",
                "你收藏的号码中奖啦");

        assertThat(response.id()).isEqualTo(88L);
        verify(notificationRepository, never()).insert(any());
    }

    /**
     * 验证分页查询会规范页码和页大小。
     */
    @Test
    void listNotificationsReturnsNormalizedPage() {
        UserNotification notification = notification(88L, 10L, "UNREAD");
        when(notificationRepository.countByUserId(10L)).thenReturn(1L);
        when(notificationRepository.findPageByUserId(10L, 1, 100)).thenReturn(List.of(notification));
        UserNotificationService service = new UserNotificationService(notificationRepository);

        var response = service.listNotifications(10L, 0, 200);

        assertThat(response.pageNo()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(100);
        assertThat(response.total()).isEqualTo(1L);
        assertThat(response.pages()).isEqualTo(1);
        assertThat(response.notifications()).hasSize(1);
    }

    /**
     * 验证未读数量使用未读状态统计。
     */
    @Test
    void unreadCountCountsUnreadNotifications() {
        when(notificationRepository.countByUserIdAndReadStatus(10L, "UNREAD")).thenReturn(3L);
        UserNotificationService service = new UserNotificationService(notificationRepository);

        assertThat(service.unreadCount(10L)).isEqualTo(3L);
    }

    /**
     * 验证当前用户可将自己的未读通知标记为已读。
     */
    @Test
    void markAsReadUpdatesOwnedUnreadNotification() {
        UserNotification notification = notification(88L, 10L, "UNREAD");
        when(notificationRepository.findById(88L)).thenReturn(Optional.of(notification));
        UserNotificationService service = new UserNotificationService(notificationRepository);

        var response = service.markAsRead(10L, 88L);

        assertThat(notification.getReadStatus()).isEqualTo("READ");
        assertThat(notification.getReadTime()).isNotNull();
        assertThat(response.readStatus()).isEqualTo("READ");
        verify(notificationRepository).updateById(notification);
    }

    /**
     * 验证已读通知重复标记时保持幂等。
     */
    @Test
    void markAsReadReturnsReadNotificationWithoutUpdatingAgain() {
        UserNotification notification = notification(88L, 10L, "READ");
        notification.setReadTime(LocalDateTime.of(2026, 7, 21, 9, 0));
        when(notificationRepository.findById(88L)).thenReturn(Optional.of(notification));
        UserNotificationService service = new UserNotificationService(notificationRepository);

        var response = service.markAsRead(10L, 88L);

        assertThat(response.readStatus()).isEqualTo("READ");
        verify(notificationRepository, never()).updateById(any());
    }

    /**
     * 验证不能读取和修改其他用户的通知。
     */
    @Test
    void markAsReadRejectsOtherUserNotificationAsNotFound() {
        when(notificationRepository.findById(88L)).thenReturn(Optional.of(notification(88L, 99L, "UNREAD")));
        UserNotificationService service = new UserNotificationService(notificationRepository);

        assertThatThrownBy(() -> service.markAsRead(10L, 88L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 验证全部标记已读会更新当前用户所有未读通知。
     */
    @Test
    void markAllAsReadUpdatesAllUnreadNotifications() {
        UserNotification first = notification(88L, 10L, "UNREAD");
        UserNotification second = notification(89L, 10L, "UNREAD");
        when(notificationRepository.findUnreadByUserId(10L, "UNREAD")).thenReturn(List.of(first, second));
        UserNotificationService service = new UserNotificationService(notificationRepository);

        int updatedCount = service.markAllAsRead(10L);

        assertThat(updatedCount).isEqualTo(2);
        assertThat(first.getReadStatus()).isEqualTo("READ");
        assertThat(second.getReadStatus()).isEqualTo("READ");
        verify(notificationRepository).updateById(first);
        verify(notificationRepository).updateById(second);
    }

    /**
     * 验证缺少用户 ID 时返回未登录错误。
     */
    @Test
    void listNotificationsRejectsMissingUserId() {
        UserNotificationService service = new UserNotificationService(notificationRepository);

        assertThatThrownBy(() -> service.listNotifications(null, 1, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 验证创建通知时必填字段不能为空。
     */
    @Test
    void createNotificationRejectsBlankBusinessKey() {
        UserNotificationService service = new UserNotificationService(notificationRepository);

        assertThatThrownBy(() -> service.createNotification(
                10L,
                NotificationType.FAVORITE_WINNING,
                NotificationBusinessType.LOTTERY_FAVORITE_WINNING,
                " ",
                "收藏号码中奖提醒",
                "你收藏的号码中奖啦"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    private UserNotification notification(Long id, Long userId, String readStatus) {
        UserNotification notification = new UserNotification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setNotificationType(NotificationType.FAVORITE_WINNING.code());
        notification.setBusinessType(NotificationBusinessType.LOTTERY_FAVORITE_WINNING.code());
        notification.setBusinessKey("DLT:26076:FAVORITE:101");
        notification.setTitle("收藏号码中奖提醒");
        notification.setContent("你收藏的号码中奖啦");
        notification.setReadStatus(readStatus);
        notification.setCreateTime(LocalDateTime.of(2026, 7, 21, 8, 0));
        notification.setUpdateTime(LocalDateTime.of(2026, 7, 21, 8, 0));
        return notification;
    }
}
