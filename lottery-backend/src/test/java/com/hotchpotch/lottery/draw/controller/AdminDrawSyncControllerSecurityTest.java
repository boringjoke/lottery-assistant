package com.hotchpotch.lottery.draw.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.config.SecurityConfig;
import com.hotchpotch.lottery.config.SyncProperties;
import com.hotchpotch.lottery.draw.record.LotteryDrawSyncResult;
import com.hotchpotch.lottery.draw.record.LotterySyncTaskPageResponse;
import com.hotchpotch.lottery.draw.record.LotterySyncTaskStatisticsResponse;
import com.hotchpotch.lottery.draw.service.LotteryDrawSyncTaskService;
import com.hotchpotch.lottery.draw.service.LotteryDrawSyncService;
import com.hotchpotch.lottery.user.record.AuthSession;
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

@WebMvcTest(AdminDrawSyncController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class AdminDrawSyncControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LotteryDrawSyncService syncService;

    @MockitoBean
    private LotteryDrawSyncTaskService syncTaskService;

    @MockitoBean
    private SyncProperties syncProperties;

    @MockitoBean
    private AuthSessionService authSessionService;

    @BeforeEach
    void setUpAdminSession() {
        when(authSessionService.findSession("admin-token"))
                .thenReturn(Optional.of(session("admin-token", List.of("USER", "ADMIN"))));
    }

    /**
     * 验证管理端同步接口未登录时返回 401。
     */
    @Test
    void syncLatestDrawRejectsAnonymousAccess() throws Exception {
        mockMvc.perform(post("/api/admin/draws/sync/latest"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    /**
     * 验证普通用户访问管理端同步接口返回 403。
     */
    @Test
    void syncLatestDrawRejectsUserRole() throws Exception {
        when(authSessionService.findSession("user-token"))
                .thenReturn(Optional.of(session("user-token", List.of("USER"))));

        mockMvc.perform(post("/api/admin/draws/sync/latest")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    /**
     * 验证管理员可以访问管理端同步接口。
     */
    @Test
    void syncLatestDrawAllowsAdminAccess() throws Exception {
        when(syncService.syncLatestDraw("ADMIN")).thenReturn(new LotteryDrawSyncResult(
                "DLT-LATEST-001",
                "DLT",
                "26076",
                "SUCCESS",
                1,
                0,
                0));

        mockMvc.perform(post("/api/admin/draws/sync/latest")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    /**
     * 验证本地管理端历史分页同步接口不需要 Basic Auth 也可以通过安全过滤链。
     */
    @Test
    void syncHistoryPageAllowsLocalManualCallWithoutBasicAuth() throws Exception {
        when(syncService.syncHistoryPage(1, 20, "ADMIN")).thenReturn(new LotteryDrawSyncResult(
                "DLT-HISTORY-PAGE-001",
                "DLT",
                null,
                "SUCCESS",
                1,
                1,
                0));

        mockMvc.perform(post("/api/admin/draws/sync/historyPage")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .param("pageNo", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    /**
     * 验证旧的批量和全量历史同步接口已经移除。
     */
    @Test
    void removedHistoryPagesAndHistoryAllEndpointsReturnNotFound() throws Exception {
        mockMvc.perform(post("/api/admin/draws/sync/historyPages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/admin/draws/sync/historyAll")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isNotFound());
    }

    /**
     * 验证本地管理端统一异步历史同步接口不需要 Basic Auth 也可以通过安全过滤链。
     */
    @Test
    void syncHistoryAllowsLocalManualCallWithoutBasicAuth() throws Exception {
        when(syncProperties.maxPagesPerTask()).thenReturn(20);
        when(syncProperties.defaultPageDelayMillis()).thenReturn(2000);
        when(syncService.startHistorySync(1, 20, 10, 2000, true, "ADMIN"))
                .thenReturn(new LotteryDrawSyncResult(
                        "DLT-HISTORY-ASYNC-001",
                        "DLT",
                        null,
                        "PENDING",
                        0,
                        0,
                        0));

        mockMvc.perform(post("/api/admin/draws/sync/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "startPage": 1,
                                    "pageSize": 20,
                                    "maxPages": 10,
                                    "pageDelayMillis": 2000,
                                    "stopWhenLastPage": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    /**
     * 验证本地管理端同步任务列表接口不需要 Basic Auth 也可以通过安全过滤链。
     */
    @Test
    void listSyncTasksAllowsLocalManualCallWithoutBasicAuth() throws Exception {
        when(syncService.listSyncTasks(1, 20, "FAILED")).thenReturn(new LotterySyncTaskPageResponse(
                1,
                20,
                0L,
                0,
                "FAILED",
                List.of()));

        mockMvc.perform(post("/api/admin/draws/sync/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "pageNo": 1,
                                    "pageSize": 20,
                                    "status": "FAILED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FAILED"));
    }

    /**
     * 验证本地管理端同步任务统计接口不需要 Basic Auth 也可以通过安全过滤链。
     */
    @Test
    void getSyncTaskStatisticsAllowsLocalManualCallWithoutBasicAuth() throws Exception {
        when(syncService.getSyncTaskStatistics()).thenReturn(new LotterySyncTaskStatisticsResponse(
                1L,
                0L,
                2L,
                3L,
                LocalDateTime.of(2026, 7, 16, 10, 0),
                LocalDateTime.of(2026, 7, 16, 11, 0),
                "crawler timeout"));

        mockMvc.perform(get("/api/admin/draws/sync/tasks/statistics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runningCount").value(1))
                .andExpect(jsonPath("$.data.latestFailureMessage").value("crawler timeout"));
    }

    /**
     * 验证本地管理端失败任务重试接口不需要 Basic Auth 也可以通过安全过滤链。
     */
    @Test
    void retrySyncTaskAllowsLocalManualCallWithoutBasicAuth() throws Exception {
        when(syncService.retrySyncTask("DLT-HISTORY-FAILED-001", "ADMIN"))
                .thenReturn(new LotteryDrawSyncResult(
                        "DLT-HISTORY-RETRY-001",
                        "DLT",
                        null,
                        "PENDING",
                        0,
                        0,
                        0));

        mockMvc.perform(post("/api/admin/draws/sync/tasks/DLT-HISTORY-FAILED-001/retry")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    private AuthSession session(String token, List<String> roles) {
        return new AuthSession(
                token,
                10L,
                "管理员",
                null,
                roles,
                LocalDateTime.of(2026, 7, 18, 12, 0));
    }
}


