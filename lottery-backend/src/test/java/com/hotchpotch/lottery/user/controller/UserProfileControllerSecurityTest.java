package com.hotchpotch.lottery.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.config.SecurityConfig;
import com.hotchpotch.lottery.user.record.AuthSession;
import com.hotchpotch.lottery.user.record.UserProfileResponse;
import com.hotchpotch.lottery.user.security.CurrentUserContext;
import com.hotchpotch.lottery.user.service.AuthSessionService;
import com.hotchpotch.lottery.user.service.UserProfileService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, CurrentUserContext.class})
class UserProfileControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private AuthSessionService authSessionService;

    @BeforeEach
    void setUpUserSession() {
        when(authSessionService.findSession("user-token"))
                .thenReturn(Optional.of(session("user-token", List.of("USER"))));
    }

    /**
     * 验证个人中心接口未登录时返回 401。
     */
    @Test
    void profileRejectsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/user/profile/detail"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    /**
     * 验证普通登录用户可以访问个人中心详情接口。
     */
    @Test
    void profileAllowsUserAccess() throws Exception {
        when(userProfileService.getProfile(10L)).thenReturn(response());

        mockMvc.perform(get("/api/user/profile/detail")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("normal"));
    }

    /**
     * 验证普通登录用户可以修改个人中心资料。
     */
    @Test
    void profileUpdateAllowsUserAccess() throws Exception {
        when(userProfileService.updateProfile(10L, new com.hotchpotch.lottery.user.record.UserProfileUpdateRequest(
                "新昵称",
                null,
                null,
                null)))
                .thenReturn(response());

        mockMvc.perform(post("/api/user/profile/update")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nickname": "新昵称",
                                    "avatarUrl": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private AuthSession session(String token, List<String> roles) {
        return new AuthSession(
                token,
                10L,
                "普通用户",
                null,
                roles,
                LocalDateTime.of(2026, 7, 18, 12, 0));
    }

    private UserProfileResponse response() {
        return new UserProfileResponse(
                10L,
                "普通用户",
                null,
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
