package com.hotchpotch.lottery.draw.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.draw.record.LotteryDrawSyncResult;
import com.hotchpotch.lottery.draw.service.LotteryDrawSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminDrawSyncControllerTest {

    /**
     * 验证管理端最新开奖同步接口会调用同步服务，并返回统一成功响应。
     */
    @Test
    void syncLatestDrawTriggersServiceAndReturnsSuccessResponse() throws Exception {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        LotteryDrawSyncResult result = new LotteryDrawSyncResult(
                "DLT-LATEST-001",
                "DLT",
                "26076",
                "SUCCESS",
                1,
                0,
                0);
        when(syncService.syncLatestDraw("ADMIN")).thenReturn(result);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminDrawSyncController(syncService))
                .build();

        mockMvc.perform(post("/api/admin/draws/sync/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.taskNo").value("DLT-LATEST-001"))
                .andExpect(jsonPath("$.data.lotteryType").value("DLT"))
                .andExpect(jsonPath("$.data.issueNo").value("26076"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.skippedCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(0));

        verify(syncService).syncLatestDraw("ADMIN");
    }

    /**
     * 验证管理端历史分页同步接口会传递分页参数，并返回统一成功响应。
     */
    @Test
    void syncHistoryPageTriggersServiceAndReturnsSuccessResponse() throws Exception {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        LotteryDrawSyncResult result = new LotteryDrawSyncResult(
                "DLT-HISTORY-PAGE-001",
                "DLT",
                null,
                "SUCCESS",
                1,
                1,
                0);
        when(syncService.syncHistoryPage(1, 20, "ADMIN")).thenReturn(result);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminDrawSyncController(syncService))
                .build();

        mockMvc.perform(post("/api/admin/draws/sync/historyPage")
                        .param("pageNo", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskNo").value("DLT-HISTORY-PAGE-001"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.skippedCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0));

        verify(syncService).syncHistoryPage(1, 20, "ADMIN");
    }
}
