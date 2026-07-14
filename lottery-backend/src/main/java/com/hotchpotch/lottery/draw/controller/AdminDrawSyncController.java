package com.hotchpotch.lottery.draw.controller;

import com.hotchpotch.lottery.common.constant.PageConstants;
import com.hotchpotch.lottery.common.response.ApiResponse;
import com.hotchpotch.lottery.config.SyncProperties;
import com.hotchpotch.lottery.draw.enums.LotterySyncTriggerSource;
import com.hotchpotch.lottery.draw.record.LotteryDrawSyncResult;
import com.hotchpotch.lottery.draw.record.LotteryHistorySyncRequest;
import com.hotchpotch.lottery.draw.record.LotterySyncTaskPageRequest;
import com.hotchpotch.lottery.draw.record.LotterySyncTaskPageResponse;
import com.hotchpotch.lottery.draw.record.LotterySyncTaskResponse;
import com.hotchpotch.lottery.draw.service.LotteryDrawSyncAsyncService;
import com.hotchpotch.lottery.draw.service.LotteryDrawSyncService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端开奖同步接口。
 */
@RestController
@RequestMapping("/api/admin/draws/sync")
public class AdminDrawSyncController {

    private final LotteryDrawSyncService syncService;
    private final LotteryDrawSyncAsyncService syncAsyncService;
    private final SyncProperties syncProperties;

    /**
     * 初始化管理端开奖同步接口依赖的同步服务。
     */
    public AdminDrawSyncController(
            LotteryDrawSyncService syncService,
            LotteryDrawSyncAsyncService syncAsyncService,
            SyncProperties syncProperties) {
        this.syncService = syncService;
        this.syncAsyncService = syncAsyncService;
        this.syncProperties = syncProperties;
    }

    /**
     * 手动触发同步最新一期大乐透开奖数据。
     */
    @PostMapping("/latest")
    public ApiResponse<LotteryDrawSyncResult> syncLatestDraw() {
        return ApiResponse.success(syncService.syncLatestDraw(LotterySyncTriggerSource.ADMIN.code()));
    }

    /**
     * 手动触发同步一页大乐透历史开奖数据。
     */
    @PostMapping("/historyPage")
    public ApiResponse<LotteryDrawSyncResult> syncHistoryPage(
            @RequestParam(defaultValue = PageConstants.DEFAULT_PAGE_NO_TEXT) int pageNo,
            @RequestParam(defaultValue = PageConstants.DEFAULT_PAGE_SIZE_TEXT) int pageSize) {
        return ApiResponse.success(syncService.syncHistoryPage(
                pageNo,
                pageSize,
                LotterySyncTriggerSource.ADMIN.code()));
    }

    /**
     * 手动创建统一异步历史同步任务，并立即返回任务编号。
     */
    @PostMapping("/history")
    public ApiResponse<LotteryDrawSyncResult> syncHistory(
            @RequestBody(required = false) LotteryHistorySyncRequest request) {
        int resolvedStartPage = defaultIfNull(
                request == null ? null : request.startPage(),
                PageConstants.DEFAULT_PAGE_NO);
        int resolvedPageSize = defaultIfNull(
                request == null ? null : request.pageSize(),
                PageConstants.DEFAULT_PAGE_SIZE);
        int resolvedMaxPages = Math.min(
                defaultIfNull(request == null ? null : request.maxPages(), syncProperties.maxPagesPerTask()),
                syncProperties.maxPagesPerTask());
        int resolvedPageDelayMillis = defaultIfNull(
                request == null ? null : request.pageDelayMillis(),
                syncProperties.defaultPageDelayMillis());
        boolean resolvedStopWhenLastPage = defaultIfNull(
                request == null ? null : request.stopWhenLastPage(),
                true);
        LotteryDrawSyncResult result = syncService.startHistorySync(
                resolvedStartPage,
                resolvedPageSize,
                resolvedMaxPages,
                resolvedPageDelayMillis,
                resolvedStopWhenLastPage,
                LotterySyncTriggerSource.ADMIN.code());
        syncAsyncService.runHistoryTask(result.taskNo());
        return ApiResponse.success(result);
    }

    /**
     * 按任务编号查询同步任务进度。
     */
    @GetMapping("/tasks/{taskNo}")
    public ApiResponse<LotterySyncTaskResponse> getSyncTask(@PathVariable String taskNo) {
        return ApiResponse.success(syncService.findSyncTask(taskNo));
    }

    /**
     * 分页查询同步任务列表，可按状态筛选。
     */
    @PostMapping("/tasks")
    public ApiResponse<LotterySyncTaskPageResponse> listSyncTasks(
            @RequestBody(required = false) LotterySyncTaskPageRequest request) {
        int resolvedPageNo = defaultIfNull(
                request == null ? null : request.pageNo(),
                PageConstants.DEFAULT_PAGE_NO);
        int resolvedPageSize = defaultIfNull(
                request == null ? null : request.pageSize(),
                PageConstants.DEFAULT_PAGE_SIZE);
        String resolvedStatus = request == null ? null : request.status();
        return ApiResponse.success(syncService.listSyncTasks(resolvedPageNo, resolvedPageSize, resolvedStatus));
    }

    /**
     * 从失败页重试历史同步任务，并立即返回新的任务编号。
     */
    @PostMapping("/tasks/{taskNo}/retry")
    public ApiResponse<LotteryDrawSyncResult> retrySyncTask(@PathVariable String taskNo) {
        LotteryDrawSyncResult result = syncService.retryHistorySync(
                taskNo,
                LotterySyncTriggerSource.ADMIN.code());
        syncAsyncService.runHistoryTask(result.taskNo());
        return ApiResponse.success(result);
    }

    /**
     * 将空整数参数转换为默认值。
     */
    private int defaultIfNull(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 将空布尔参数转换为默认值。
     */
    private boolean defaultIfNull(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }
}
