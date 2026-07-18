package com.hotchpotch.lottery.user.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.common.response.GlobalExceptionHandler;
import com.hotchpotch.lottery.config.AuthProperties;
import com.hotchpotch.lottery.user.record.AuthSession;
import com.hotchpotch.lottery.user.record.LoginResponse;
import com.hotchpotch.lottery.user.record.PasswordLoginRequest;
import com.hotchpotch.lottery.user.service.AuthSessionService;
import com.hotchpotch.lottery.user.service.UserAuthService;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {

    @Test
    void loginWithPasswordCreatesSessionAndWritesHttpOnlyCookie() throws Exception {
        UserAuthService userAuthService = mock(UserAuthService.class);
        AuthSessionService authSessionService = mock(AuthSessionService.class);
        AuthProperties authProperties = new AuthProperties();
        authProperties.setSessionTtlSeconds(3600);
        LoginResponse loginResponse = new LoginResponse(
                10L,
                "管理员",
                "https://example.com/avatar.png",
                List.of("USER", "ADMIN"));
        AuthSession session = session("token-001");
        when(userAuthService.loginWithPassword(new PasswordLoginRequest("admin", "Admin@123456")))
                .thenReturn(loginResponse);
        when(authSessionService.createSession(loginResponse)).thenReturn(session);
        MockMvc mockMvc = newMockMvc(userAuthService, authSessionService, authProperties);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "account": "admin",
                                    "password": "Admin@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("LOTTERY_AUTH_TOKEN=token-001")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("SameSite=Lax")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("token-001"))
                .andExpect(jsonPath("$.data.userId").value(10))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"))
                .andExpect(jsonPath("$.data.roles[1]").value("ADMIN"));

        verify(userAuthService).loginWithPassword(new PasswordLoginRequest("admin", "Admin@123456"));
        verify(authSessionService).createSession(loginResponse);
    }

    @Test
    void currentUserReadsBearerToken() throws Exception {
        AuthSessionService authSessionService = mock(AuthSessionService.class);
        when(authSessionService.findSession("token-001")).thenReturn(Optional.of(session("token-001")));
        MockMvc mockMvc = newMockMvc(mock(UserAuthService.class), authSessionService, new AuthProperties());

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(10))
                .andExpect(jsonPath("$.data.nickname").value("管理员"))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"));

        verify(authSessionService).findSession("token-001");
    }

    @Test
    void currentUserReadsCookieToken() throws Exception {
        AuthSessionService authSessionService = mock(AuthSessionService.class);
        when(authSessionService.findSession("cookie-token")).thenReturn(Optional.of(session("cookie-token")));
        MockMvc mockMvc = newMockMvc(mock(UserAuthService.class), authSessionService, new AuthProperties());

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie("LOTTERY_AUTH_TOKEN", "cookie-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(10));

        verify(authSessionService).findSession("cookie-token");
    }

    @Test
    void currentUserRejectsMissingToken() throws Exception {
        MockMvc mockMvc = newMockMvc(mock(UserAuthService.class), mock(AuthSessionService.class), new AuthProperties());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void logoutDeletesSessionAndClearsCookie() throws Exception {
        AuthSessionService authSessionService = mock(AuthSessionService.class);
        MockMvc mockMvc = newMockMvc(mock(UserAuthService.class), authSessionService, new AuthProperties());

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-001"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("LOTTERY_AUTH_TOKEN=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")))
                .andExpect(jsonPath("$.success").value(true));

        verify(authSessionService).deleteSession("token-001");
    }

    private MockMvc newMockMvc(
            UserAuthService userAuthService,
            AuthSessionService authSessionService,
            AuthProperties authProperties) {
        return MockMvcBuilders
                .standaloneSetup(new AuthController(userAuthService, authSessionService, authProperties))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AuthSession session(String token) {
        return new AuthSession(
                token,
                10L,
                "管理员",
                "https://example.com/avatar.png",
                List.of("USER", "ADMIN"),
                LocalDateTime.of(2026, 7, 18, 12, 0));
    }
}
