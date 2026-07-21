package com.hotchpotch.lottery.user.controller;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.user.record.UserProfileResponse;
import com.hotchpotch.lottery.user.record.UserProfileUpdateRequest;
import com.hotchpotch.lottery.user.security.CurrentUserContext;
import com.hotchpotch.lottery.user.service.AuthSessionService;
import com.hotchpotch.lottery.user.service.UserProfileService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private CurrentUserContext currentUserContext;

    @MockitoBean
    private AuthSessionService authSessionService;

    @BeforeEach
    void setUpCurrentUser() {
        when(currentUserContext.requireUserId()).thenReturn(10L);
        when(currentUserContext.requireToken()).thenReturn("token-001");
    }

    /**
     * 验证个人中心详情接口使用当前登录用户 ID。
     */
    @Test
    void getProfileUsesCurrentUserId() throws Exception {
        when(userProfileService.getProfile(10L)).thenReturn(response("普通用户", null));

        mockMvc.perform(get("/api/user/profile/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(10))
                .andExpect(jsonPath("$.data.maskedPhone").value("138****8000"));
    }

    /**
     * 验证个人中心修改接口使用请求体。
     */
    @Test
    void updateProfileUsesRequestBody() throws Exception {
        when(userProfileService.updateProfile(10L, new UserProfileUpdateRequest(
                "新昵称",
                "/avatars/avatar-02.svg",
                true,
                "user@example.com")))
                .thenReturn(response("新昵称", "/avatars/avatar-02.svg"));

        mockMvc.perform(post("/api/user/profile/update")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nickname": "新昵称",
                                    "avatarUrl": "/avatars/avatar-02.svg",
                                    "emailNotificationEnabled": true,
                                    "notificationEmail": "user@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("新昵称"))
                .andExpect(jsonPath("$.data.avatarUrl").value("/avatars/avatar-02.svg"))
                .andExpect(jsonPath("$.data.emailNotificationEnabled").value(false));
        verify(authSessionService).updateSessionProfile("token-001", "新昵称", "/avatars/avatar-02.svg");
    }

    private UserProfileResponse response(String nickname, String avatarUrl) {
        return new UserProfileResponse(
                10L,
                nickname,
                avatarUrl,
                "ACTIVE",
                List.of("USER"),
                "normal",
                "138****8000",
                "n****l@example.com",
                false,
                LocalDateTime.of(2026, 7, 18, 10, 0),
                LocalDateTime.of(2026, 7, 18, 12, 0));
    }
}
