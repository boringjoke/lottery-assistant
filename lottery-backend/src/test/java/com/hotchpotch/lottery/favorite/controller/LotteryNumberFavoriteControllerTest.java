package com.hotchpotch.lottery.favorite.controller;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoriteCreateRequest;
import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoritePageResponse;
import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoriteResponse;
import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoriteUpdateRequest;
import com.hotchpotch.lottery.favorite.service.LotteryNumberFavoriteService;
import com.hotchpotch.lottery.user.security.CurrentUserContext;
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

@WebMvcTest(LotteryNumberFavoriteController.class)
@AutoConfigureMockMvc(addFilters = false)
class LotteryNumberFavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LotteryNumberFavoriteService favoriteService;

    @MockitoBean
    private CurrentUserContext currentUserContext;

    @BeforeEach
    void setUpCurrentUser() {
        when(currentUserContext.requireUserId()).thenReturn(10L);
    }

    /**
     * 验证新增收藏接口会使用当前登录用户 ID。
     */
    @Test
    void createFavoriteUsesCurrentUserId() throws Exception {
        when(favoriteService.createFavorite(
                10L,
                new LotteryNumberFavoriteCreateRequest(
                        "DLT",
                        List.of(1, 5, 12, 23, 35),
                        List.of(3, 11),
                        "蓝号观察",
                        "本地测试")))
                .thenReturn(response("ACTIVE"));

        mockMvc.perform(post("/api/lottery/favorites/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "lotteryType": "DLT",
                                    "frontNumbers": [1, 5, 12, 23, 35],
                                    "backNumbers": [3, 11],
                                    "favoriteName": "蓝号观察",
                                    "remark": "本地测试"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayText").value("01 05 12 23 35 + 03 11"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    /**
     * 验证分页查询收藏接口透传筛选条件。
     */
    @Test
    void listFavoritesPassesQueryParameters() throws Exception {
        when(favoriteService.listFavorites(10L, 1, 20, "ACTIVE", "蓝号"))
                .thenReturn(new LotteryNumberFavoritePageResponse(
                        1,
                        20,
                        1L,
                        1,
                        "ACTIVE",
                        "蓝号",
                        List.of(response("ACTIVE"))));

        mockMvc.perform(get("/api/lottery/favorites/page")
                        .param("pageNo", "1")
                        .param("pageSize", "20")
                        .param("status", "ACTIVE")
                        .param("keyword", "蓝号"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.favorites[0].id").value(20));
    }

    /**
     * 验证收藏详情接口。
     */
    @Test
    void getFavoriteReturnsDetail() throws Exception {
        when(favoriteService.getFavorite(10L, 20L)).thenReturn(response("ACTIVE"));

        mockMvc.perform(get("/api/lottery/favorites/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(20));
    }

    /**
     * 验证修改收藏接口。
     */
    @Test
    void updateFavoriteReturnsUpdatedFavorite() throws Exception {
        when(favoriteService.updateFavorite(10L, 20L, new LotteryNumberFavoriteUpdateRequest(20L, "新名称", "新备注")))
                .thenReturn(response("ACTIVE"));

        mockMvc.perform(post("/api/lottery/favorites/update")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "favoriteId": 20,
                                    "favoriteName": "新名称",
                                    "remark": "新备注"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    /**
     * 验证取消收藏接口。
     */
    @Test
    void deactivateFavoriteReturnsCancelledFavorite() throws Exception {
        when(favoriteService.deactivateFavorite(10L, 20L)).thenReturn(response("CANCELLED"));

        mockMvc.perform(post("/api/lottery/favorites/deactivate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "favoriteId": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    /**
     * 验证重新启用收藏接口。
     */
    @Test
    void activateFavoriteReturnsActiveFavorite() throws Exception {
        when(favoriteService.activateFavorite(10L, 20L)).thenReturn(response("ACTIVE"));

        mockMvc.perform(post("/api/lottery/favorites/activate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "favoriteId": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    /**
     * 验证删除收藏接口使用请求体中的收藏 ID 和当前登录用户 ID。
     */
    @Test
    void deleteFavoriteUsesCurrentUserIdAndRequestBody() throws Exception {
        mockMvc.perform(post("/api/lottery/favorites/delete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "favoriteId": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(favoriteService).deleteFavorite(10L, 20L);
    }

    private LotteryNumberFavoriteResponse response(String status) {
        return new LotteryNumberFavoriteResponse(
                20L,
                "DLT",
                "01,05,12,23,35",
                "03,11",
                "01 05 12 23 35 + 03 11",
                "蓝号观察",
                "本地测试",
                status,
                LocalDateTime.of(2026, 7, 18, 10, 0),
                LocalDateTime.of(2026, 7, 18, 10, 0),
                null);
    }
}
