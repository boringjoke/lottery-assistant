package com.hotchpotch.lottery.favorite.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.config.SecurityConfig;
import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoritePageResponse;
import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoriteResponse;
import com.hotchpotch.lottery.favorite.service.LotteryNumberFavoriteService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LotteryNumberFavoriteController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, CurrentUserContext.class})
class LotteryNumberFavoriteControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LotteryNumberFavoriteService favoriteService;

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
     * 验证收藏接口未登录时返回 401。
     */
    @Test
    void favoritesRejectAnonymousAccess() throws Exception {
        mockMvc.perform(post("/api/lottery/favorites/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "lotteryType": "DLT",
                                    "frontNumbers": [1, 5, 12, 23, 35],
                                    "backNumbers": [3, 11]
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    /**
     * 验证普通登录用户可以访问收藏列表接口。
     */
    @Test
    void favoritesAllowUserAccess() throws Exception {
        when(favoriteService.listFavorites(10L, 1, 20, "ACTIVE", null))
                .thenReturn(new LotteryNumberFavoritePageResponse(
                        1,
                        20,
                        1L,
                        1,
                        "ACTIVE",
                        null,
                        List.of(response())));

        mockMvc.perform(get("/api/lottery/favorites/page")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    /**
     * 验证管理员也具备收藏接口访问能力。
     */
    @Test
    void favoritesAllowAdminAccess() throws Exception {
        when(favoriteService.listFavorites(10L, 1, 20, null, null))
                .thenReturn(new LotteryNumberFavoritePageResponse(
                        1,
                        20,
                        0L,
                        0,
                        null,
                        null,
                        List.of()));

        mockMvc.perform(get("/api/lottery/favorites/page")
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
                LocalDateTime.of(2026, 7, 18, 12, 0));
    }

    private LotteryNumberFavoriteResponse response() {
        return new LotteryNumberFavoriteResponse(
                20L,
                "DLT",
                "01,05,12,23,35",
                "03,11",
                "01 05 12 23 35 + 03 11",
                "蓝号观察",
                "本地测试",
                "ACTIVE",
                LocalDateTime.of(2026, 7, 18, 10, 0),
                LocalDateTime.of(2026, 7, 18, 10, 0),
                null);
    }
}
