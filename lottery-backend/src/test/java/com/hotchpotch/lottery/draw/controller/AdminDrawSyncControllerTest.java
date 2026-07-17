package com.hotchpotch.lottery.draw.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.config.SyncProperties;
import com.hotchpotch.lottery.common.response.GlobalExceptionHandler;
import com.hotchpotch.lottery.draw.record.LotteryDrawSyncResult;
import com.hotchpotch.lottery.draw.record.LotterySyncTaskPageResponse;
import com.hotchpotch.lottery.draw.record.LotterySyncTaskResponse;
import com.hotchpotch.lottery.draw.record.LotterySyncTaskStatisticsResponse;
import com.hotchpotch.lottery.draw.service.LotteryDrawSyncTaskService;
import com.hotchpotch.lottery.draw.service.LotteryDrawSyncService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
        LotteryDrawSyncTaskService syncTaskService = mock(LotteryDrawSyncTaskService.class);
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
                .standaloneSetup(newController(syncService, syncTaskService))
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
        LotteryDrawSyncTaskService syncTaskService = mock(LotteryDrawSyncTaskService.class);
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
                .standaloneSetup(newController(syncService, syncTaskService))
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
        LotteryDrawSyncTaskService syncTaskService = mock(LotteryDrawSyncTaskService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(newController(syncService, syncTaskService))
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
        LotteryDrawSyncTaskService syncTaskService = mock(LotteryDrawSyncTaskService.class);
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
                .standaloneSetup(newController(syncService, syncTaskService))
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
        verify(syncTaskService).runTask("DLT-HISTORY-ASYNC-001");
    }

    /**
     * 验证按期号范围同步接口支持从 JSON 请求体创建异步任务。
     */
    @Test
    void syncIssueRangeCreatesAsyncTaskFromJsonBodyAndReturnsPendingResponse() throws Exception {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        LotteryDrawSyncTaskService syncTaskService = mock(LotteryDrawSyncTaskService.class);
        LotteryDrawSyncResult result = new LotteryDrawSyncResult(
                "DLT-ISSUE-RANGE-ASYNC-001",
                "DLT",
                null,
                "PENDING",
                0,
                0,
                0);
        when(syncService.startIssueRangeSync("26070", "26076", 1, 20, 20, 5000, true, "ADMIN"))
                .thenReturn(result);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(newController(syncService, syncTaskService))
                .build();

        mockMvc.perform(post("/api/admin/draws/sync/issueRange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "startIssueNo": "26070",
                                    "endIssueNo": "26076",
                                    "startPage": 1,
                                    "pageSize": 20,
                                    "maxPages": 3,
                                    "pageDelayMillis": 5000,
                                    "stopWhenLastPage": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskNo").value("DLT-ISSUE-RANGE-ASYNC-001"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(syncService).startIssueRangeSync("26070", "26076", 1, 20, 20, 5000, true, "ADMIN");
        verify(syncTaskService).runTask("DLT-ISSUE-RANGE-ASYNC-001");
    }

    /**
     * 验证按期号范围同步接口会拒绝空请求体，避免空指针进入业务逻辑。
     */
    @Test
    void syncIssueRangeRejectsEmptyBody() throws Exception {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        LotteryDrawSyncTaskService syncTaskService = mock(LotteryDrawSyncTaskService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(newController(syncService, syncTaskService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/admin/draws/sync/issueRange"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("期号范围同步请求体不能为空"));

        verify(syncService, never()).startIssueRangeSync(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.any());
    }

    /**
     * 验证按开奖日期范围同步接口支持从 JSON 请求体创建异步任务。
     */
    @Test
    void syncDateRangeCreatesAsyncTaskFromJsonBodyAndReturnsPendingResponse() throws Exception {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        LotteryDrawSyncTaskService syncTaskService = mock(LotteryDrawSyncTaskService.class);
        LotteryDrawSyncResult result = new LotteryDrawSyncResult(
                "DLT-DATE-RANGE-ASYNC-001",
                "DLT",
                null,
                "PENDING",
                0,
                0,
                0);
        when(syncService.startDateRangeSync(
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalDate.of(2026, 7, 11),
                1,
                20,
                20,
                5000,
                true,
                "ADMIN")).thenReturn(result);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(newController(syncService, syncTaskService))
                .build();

        mockMvc.perform(post("/api/admin/draws/sync/dateRange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "startDate": "2026-07-01",
                                    "endDate": "2026-07-11",
                                    "startPage": 1,
                                    "pageSize": 20,
                                    "maxPages": 3,
                                    "pageDelayMillis": 5000,
                                    "stopWhenLastPage": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskNo").value("DLT-DATE-RANGE-ASYNC-001"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(syncService).startDateRangeSync(
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalDate.of(2026, 7, 11),
                1,
                20,
                20,
                5000,
                true,
                "ADMIN");
        verify(syncTaskService).runTask("DLT-DATE-RANGE-ASYNC-001");
    }

    /**
     * 验证按日期范围同步接口会拒绝空请求体，避免空指针进入业务逻辑。
     */
    @Test
    void syncDateRangeRejectsEmptyBody() throws Exception {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        LotteryDrawSyncTaskService syncTaskService = mock(LotteryDrawSyncTaskService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(newController(syncService, syncTaskService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/admin/draws/sync/dateRange"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("日期范围同步请求体不能为空"));

        verify(syncService, never()).startDateRangeSync(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.any());
    }

    /**
     * 验证同步任务查询接口会按任务编号返回任务进度。
     */
    @Test
    void getSyncTaskReturnsTaskProgressResponse() throws Exception {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        LotteryDrawSyncTaskService syncTaskService = mock(LotteryDrawSyncTaskService.class);
        LotterySyncTaskResponse response = new LotterySyncTaskResponse(
                "DLT-HISTORY-ASYNC-001",
                "DLT",
                "HISTORY",
                "ADMIN",
                "RUNNING",
                "{\"startPage\":1,\"pageSize\":20}",
                Map.of("startPage", "1", "pageSize", "20"),
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
                .standaloneSetup(newController(syncService, syncTaskService))
                .build();

        mockMvc.perform(get("/api/admin/draws/sync/tasks/DLT-HISTORY-ASYNC-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskNo").value("DLT-HISTORY-ASYNC-001"))
                .andExpect(jsonPath("$.data.syncType").value("HISTORY"))
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.requestParams").value("{\"startPage\":1,\"pageSize\":20}"))
                .andExpect(jsonPath("$.data.requestParamMap.startPage").value("1"))
                .andExpect(jsonPath("$.data.requestParamMap.pageSize").value("20"))
                .andExpect(jsonPath("$.data.currentPage").value(3))
                .andExpect(jsonPath("$.data.lastSuccessPage").value(2))
                .andExpect(jsonPath("$.data.successCount").value(40));

        verify(syncService).findSyncTask("DLT-HISTORY-ASYNC-001");
    }

    /**
     * 验证同步任务列表接口会传递分页和状态筛选参数，并返回分页数据。
     */
    @Test
    void listSyncTasksReturnsPagedTaskResponse() throws Exception {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        LotteryDrawSyncTaskService syncTaskService = mock(LotteryDrawSyncTaskService.class);
        LotterySyncTaskResponse task = new LotterySyncTaskResponse(
                "DLT-HISTORY-FAILED-001",
                "DLT",
                "HISTORY",
                "ADMIN",
                "FAILED",
                "{\"startPage\":1,\"pageSize\":20}",
                Map.of("startPage", "1", "pageSize", "20"),
                1,
                3,
                2,
                3,
                20,
                10,
                2000,
                true,
                40,
                0,
                1,
                "crawler timeout",
                LocalDateTime.of(2026, 7, 13, 10, 0),
                LocalDateTime.of(2026, 7, 13, 10, 1));
        when(syncService.listSyncTasks(1, 20, "FAILED")).thenReturn(new LotterySyncTaskPageResponse(
                1,
                20,
                1L,
                1,
                "FAILED",
                List.of(task)));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(newController(syncService, syncTaskService))
                .build();

        mockMvc.perform(post("/api/admin/draws/sync/tasks")
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
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pages").value(1))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.tasks[0].taskNo").value("DLT-HISTORY-FAILED-001"))
                .andExpect(jsonPath("$.data.tasks[0].status").value("FAILED"))
                .andExpect(jsonPath("$.data.tasks[0].requestParams").value("{\"startPage\":1,\"pageSize\":20}"))
                .andExpect(jsonPath("$.data.tasks[0].requestParamMap.startPage").value("1"))
                .andExpect(jsonPath("$.data.tasks[0].requestParamMap.pageSize").value("20"))
                .andExpect(jsonPath("$.data.tasks[0].failedPage").value(3));

        verify(syncService).listSyncTasks(1, 20, "FAILED");
    }

    /**
     * 验证同步任务统计接口会返回管理页顶部需要的状态概览。
     */
    @Test
    void getSyncTaskStatisticsReturnsStatusOverview() throws Exception {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        LotteryDrawSyncTaskService syncTaskService = mock(LotteryDrawSyncTaskService.class);
        LotterySyncTaskStatisticsResponse response = new LotterySyncTaskStatisticsResponse(
                1L,
                2L,
                3L,
                4L,
                LocalDateTime.of(2026, 7, 16, 10, 0),
                LocalDateTime.of(2026, 7, 16, 11, 0),
                "crawler timeout");
        when(syncService.getSyncTaskStatistics()).thenReturn(response);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(newController(syncService, syncTaskService))
                .build();

        mockMvc.perform(get("/api/admin/draws/sync/tasks/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runningCount").value(1))
                .andExpect(jsonPath("$.data.pendingCount").value(2))
                .andExpect(jsonPath("$.data.failedCount").value(3))
                .andExpect(jsonPath("$.data.successCountToday").value(4))
                .andExpect(jsonPath("$.data.latestFailureMessage").value("crawler timeout"));

        verify(syncService).getSyncTaskStatistics();
    }

    /**
     * 验证失败任务重试接口会创建新的异步任务并提交后台执行。
     */
    @Test
    void retrySyncTaskCreatesNewAsyncTaskAndReturnsPendingResponse() throws Exception {
        LotteryDrawSyncService syncService = mock(LotteryDrawSyncService.class);
        LotteryDrawSyncTaskService syncTaskService = mock(LotteryDrawSyncTaskService.class);
        LotteryDrawSyncResult result = new LotteryDrawSyncResult(
                "DLT-HISTORY-RETRY-001",
                "DLT",
                null,
                "PENDING",
                0,
                0,
                0);
        when(syncService.retrySyncTask("DLT-HISTORY-FAILED-001", "ADMIN")).thenReturn(result);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(newController(syncService, syncTaskService))
                .build();

        mockMvc.perform(post("/api/admin/draws/sync/tasks/DLT-HISTORY-FAILED-001/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskNo").value("DLT-HISTORY-RETRY-001"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(syncService).retrySyncTask("DLT-HISTORY-FAILED-001", "ADMIN");
        verify(syncTaskService).runTask("DLT-HISTORY-RETRY-001");
    }

    /**
     * 创建使用测试同步配置的管理端同步 Controller。
     */
    private AdminDrawSyncController newController(
            LotteryDrawSyncService syncService,
            LotteryDrawSyncTaskService syncTaskService) {
        return new AdminDrawSyncController(syncService, syncTaskService, new SyncProperties());
    }
}


