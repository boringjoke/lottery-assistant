package com.hotchpotch.lottery.draw.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.config.SyncProperties;
import com.hotchpotch.lottery.draw.record.LotteryDrawSyncResult;
import com.hotchpotch.lottery.draw.record.LotterySyncTaskResponse;
import com.hotchpotch.lottery.draw.service.LotteryDrawSyncAsyncService;
import com.hotchpotch.lottery.draw.service.LotteryDrawSyncService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminDrawSyncControllerTest {

    /**
     * 验证管理端最新开奖同步接口会调用同步服务，并返回统一成功响应。
     */
    @Test
    void syncLatestDrawTriggersServiceAndReturnsSuccessResponse() throws Exception {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        LotteryDrawSyncAsyncService syncAsyncService = mock(LotteryDrawSyncAsyncService.class);
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
                .standaloneSetup(newController(syncService, syncAsyncService))
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
        LotteryDrawSyncAsyncService syncAsyncService = mock(LotteryDrawSyncAsyncService.class);
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
                .standaloneSetup(newController(syncService, syncAsyncService))
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

    /**
     * 验证旧的批量和全量历史同步接口已经移除，避免绕过异步限速保护。
     */
    @Test
    void removedHistoryPagesAndHistoryAllEndpointsReturnNotFound() throws Exception {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        LotteryDrawSyncAsyncService syncAsyncService = mock(LotteryDrawSyncAsyncService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(newController(syncService, syncAsyncService))
                .build();

        mockMvc.perform(post("/api/admin/draws/sync/historyPages"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/admin/draws/sync/historyAll"))
                .andExpect(status().isNotFound());
    }

    /**
     * 验证统一历史同步接口支持从 JSON 请求体读取任务参数。
     */
    @Test
    void syncHistoryCreatesAsyncTaskFromJsonBodyAndReturnsPendingResponse() throws Exception {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        LotteryDrawSyncAsyncService syncAsyncService = mock(LotteryDrawSyncAsyncService.class);
        LotteryDrawSyncResult result = new LotteryDrawSyncResult(
                "DLT-HISTORY-ASYNC-001",
                "DLT",
                null,
                "PENDING",
                0,
                0,
                0);
        when(syncService.startHistorySync(1, 20, 1, 5000, true, "ADMIN")).thenReturn(result);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(newController(syncService, syncAsyncService))
                .build();

        mockMvc.perform(post("/api/admin/draws/sync/history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "startPage": 1,
                                    "pageSize": 20,
                                    "maxPages": 1,
                                    "pageDelayMillis": 5000,
                                    "stopWhenLastPage": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskNo").value("DLT-HISTORY-ASYNC-001"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.skippedCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(0));

        verify(syncService).startHistorySync(1, 20, 1, 5000, true, "ADMIN");
        verify(syncAsyncService).runHistoryTask("DLT-HISTORY-ASYNC-001");
    }

    /**
     * 验证同步任务查询接口会按任务编号返回任务进度。
     */
    @Test
    void getSyncTaskReturnsTaskProgressResponse() throws Exception {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        LotteryDrawSyncAsyncService syncAsyncService = mock(LotteryDrawSyncAsyncService.class);
        LotterySyncTaskResponse response = new LotterySyncTaskResponse(
                "DLT-HISTORY-ASYNC-001",
                "DLT",
                "HISTORY",
                "ADMIN",
                "RUNNING",
                1,
                3,
                2,
                null,
                20,
                10,
                2000,
                true,
                40,
                0,
                0,
                null,
                LocalDateTime.of(2026, 7, 13, 10, 0),
                null);
        when(syncService.findSyncTask("DLT-HISTORY-ASYNC-001")).thenReturn(response);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(newController(syncService, syncAsyncService))
                .build();

        mockMvc.perform(get("/api/admin/draws/sync/tasks/DLT-HISTORY-ASYNC-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskNo").value("DLT-HISTORY-ASYNC-001"))
                .andExpect(jsonPath("$.data.syncType").value("HISTORY"))
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.currentPage").value(3))
                .andExpect(jsonPath("$.data.lastSuccessPage").value(2))
                .andExpect(jsonPath("$.data.successCount").value(40));

        verify(syncService).findSyncTask("DLT-HISTORY-ASYNC-001");
    }

    /**
     * 创建使用测试同步配置的管理端同步 Controller。
     */
    private AdminDrawSyncController newController(
            LotteryDrawSyncService syncService,
            LotteryDrawSyncAsyncService syncAsyncService) {
        return new AdminDrawSyncController(syncService, syncAsyncService, new SyncProperties());
    }
}
