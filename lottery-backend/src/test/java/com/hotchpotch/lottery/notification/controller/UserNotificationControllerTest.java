package com.hotchpotch.lottery.notification.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.notification.record.UserNotificationPageResponse;
import com.hotchpotch.lottery.notification.record.UserNotificationResponse;
import com.hotchpotch.lottery.notification.service.UserNotificationService;
import com.hotchpotch.lottery.user.security.CurrentUserContext;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserNotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserNotificationService notificationService;

    @MockitoBean
    private CurrentUserContext currentUserContext;

    @BeforeEach
    void setUpCurrentUser() {
        when(currentUserContext.requireUserId()).thenReturn(10L);
    }

    /**
     * 验证分页查询通知接口透传当前用户和分页参数。
     */
    @Test
    void listNotificationsPassesCurrentUserAndPageParameters() throws Exception {
        when(notificationService.listNotifications(10L, 2, 5))
                .thenReturn(new UserNotificationPageResponse(
                        2,
                        5,
                        1L,
                        1,
                        List.of(notification())));

        mockMvc.perform(get("/api/user/notifications/page")
                        .param("pageNo", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pageNo").value(2))
                .andExpect(jsonPath("$.data.notifications[0].title").value("收藏号码中奖提醒"));
    }

    /**
     * 验证未读数量接口。
     */
    @Test
    void unreadCountReturnsCurrentUserUnreadCount() throws Exception {
        when(notificationService.unreadCount(10L)).thenReturn(3L);

        mockMvc.perform(get("/api/user/notifications/unreadCount"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));
    }

    /**
     * 验证单条标记已读接口。
     */
    @Test
    void markAsReadUsesCurrentUserAndNotificationId() throws Exception {
        when(notificationService.markAsRead(10L, 88L)).thenReturn(notification());

        mockMvc.perform(post("/api/user/notifications/88/read").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(88))
                .andExpect(jsonPath("$.data.readStatus").value("UNREAD"));
    }

    /**
     * 验证全部标记已读接口。
     */
    @Test
    void markAllAsReadUsesCurrentUser() throws Exception {
        when(notificationService.markAllAsRead(10L)).thenReturn(2);

        mockMvc.perform(post("/api/user/notifications/readAll").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));

        verify(notificationService).markAllAsRead(10L);
    }

    private UserNotificationResponse notification() {
        return new UserNotificationResponse(
                88L,
                10L,
                "FAVORITE_WINNING",
                "LOTTERY_FAVORITE_WINNING",
                "DLT:26076:FAVORITE:101",
                "收藏号码中奖提醒",
                "你收藏的号码中奖啦",
                "UNREAD",
                null,
                LocalDateTime.of(2026, 7, 21, 8, 0),
                LocalDateTime.of(2026, 7, 21, 8, 0));
    }
}
