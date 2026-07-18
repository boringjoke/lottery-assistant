package com.hotchpotch.lottery.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.config.AuthProperties;
import com.hotchpotch.lottery.config.SecurityConfig;
import com.hotchpotch.lottery.user.record.AuthSession;
import com.hotchpotch.lottery.user.record.LoginResponse;
import com.hotchpotch.lottery.user.record.PasswordLoginRequest;
import com.hotchpotch.lottery.user.service.AuthSessionService;
import com.hotchpotch.lottery.user.service.UserAuthService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAuthService userAuthService;

    @MockitoBean
    private AuthSessionService authSessionService;

    @MockitoBean
    private AuthProperties authProperties;

    @Test
    void loginAllowsAnonymousAccess() throws Exception {
        LoginResponse loginResponse = new LoginResponse(10L, "管理员", null, List.of("USER"));
        when(userAuthService.loginWithPassword(new PasswordLoginRequest("admin", "Admin@123456")))
                .thenReturn(loginResponse);
        when(authProperties.sessionTtlSeconds()).thenReturn(604800L);
        when(authSessionService.createSession(loginResponse)).thenReturn(new AuthSession(
                "token-001",
                10L,
                "管理员",
                null,
                List.of("USER"),
                LocalDateTime.of(2026, 7, 18, 12, 0)));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "account": "admin",
                                    "password": "Admin@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("token-001"));
    }
}
