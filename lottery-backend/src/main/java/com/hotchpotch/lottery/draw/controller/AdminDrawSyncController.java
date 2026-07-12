package com.hotchpotch.lottery.draw.controller;

import com.hotchpotch.lottery.common.response.ApiResponse;
import com.hotchpotch.lottery.draw.record.LotteryDrawSyncResult;
import com.hotchpotch.lottery.draw.service.LotteryDrawSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端开奖同步接口。
 */
@RestController
@RequestMapping("/api/admin/draws/sync")
public class AdminDrawSyncController {

    private static final String TRIGGER_SOURCE_ADMIN = "ADMIN";

    private final LotteryDrawSyncService syncService;

    /**
     * 初始化管理端开奖同步接口依赖的同步服务。
     */
    public AdminDrawSyncController(LotteryDrawSyncService syncService) {
        this.syncService = syncService;
    }

    /**
     * 手动触发同步最新一期大乐透开奖数据。
     */
    @PostMapping("/latest")
    public ApiResponse<LotteryDrawSyncResult> syncLatestDraw() {
        return ApiResponse.success(syncService.syncLatestDraw(TRIGGER_SOURCE_ADMIN));
    }
}
