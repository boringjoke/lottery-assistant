package com.hotchpotch.lottery.draw.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.config.SecurityConfig;
import com.hotchpotch.lottery.config.SyncProperties;
import com.hotchpotch.lottery.draw.record.LotteryDrawSyncResult;
import com.hotchpotch.lottery.draw.service.LotteryDrawSyncAsyncService;
import com.hotchpotch.lottery.draw.service.LotteryDrawSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
    private LotteryDrawSyncAsyncService syncAsyncService;

    @MockitoBean
    private SyncProperties syncProperties;

    /**
     * 验证本地管理端同步接口不需要 Basic Auth 也可以通过安全过滤链。
     */
    @Test
    void syncLatestDrawAllowsLocalManualCallWithoutBasicAuth() throws Exception {
        when(syncService.syncLatestDraw("ADMIN")).thenReturn(new LotteryDrawSyncResult(
                "DLT-LATEST-001",
                "DLT",
                "26076",
                "SUCCESS",
                1,
                0,
                0));

        mockMvc.perform(post("/api/admin/draws/sync/latest"))
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
        mockMvc.perform(post("/api/admin/draws/sync/historyPages"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/admin/draws/sync/historyAll"))
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
}
