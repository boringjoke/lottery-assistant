package com.hotchpotch.lottery.notification.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.config.SecurityConfig;
import com.hotchpotch.lottery.notification.record.UserNotificationPageResponse;
import com.hotchpotch.lottery.notification.service.UserNotificationService;
import com.hotchpotch.lottery.user.record.AuthSession;
import com.hotchpotch.lottery.user.security.CurrentUserContext;
import com.hotchpotch.lottery.user.service.AuthSessionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserNotificationController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, CurrentUserContext.class})
class UserNotificationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserNotificationService notificationService;

    @MockitoBean
    private AuthSessionService authSessionService;

    @BeforeEach
    void setUpUserSession() {
        when(authSessionService.findSession("user-token"))
                .thenReturn(Optional.of(session("user-token", List.of("USER"))));
        when(authSessionService.findSession("admin-token"))
                .thenReturn(Optional.of(session("admin-token", List.of("USER", "ADMIN"))));
    }

    /**
     * 验证通知接口未登录时返回 401。
     */
    @Test
    void notificationsRejectAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/user/notifications/page"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    /**
     * 验证普通登录用户可以访问通知接口。
     */
    @Test
    void notificationsAllowUserAccess() throws Exception {
        when(notificationService.listNotifications(10L, 1, 20))
                .thenReturn(new UserNotificationPageResponse(1, 20, 0L, 0, List.of()));

        mockMvc.perform(get("/api/user/notifications/page")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * 验证管理员也具备通知接口访问能力。
     */
    @Test
    void notificationsAllowAdminAccess() throws Exception {
        when(notificationService.unreadCount(10L)).thenReturn(0L);

        mockMvc.perform(get("/api/user/notifications/unreadCount")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private AuthSession session(String token, List<String> roles) {
        return new AuthSession(
                token,
                10L,
                "测试用户",
                null,
                roles,
                LocalDateTime.of(2026, 7, 21, 8, 0));
    }
}
