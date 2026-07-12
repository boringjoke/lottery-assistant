package com.hotchpotch.lottery.draw.controller;

import com.hotchpotch.lottery.common.response.ApiResponse;
import com.hotchpotch.lottery.draw.record.LotteryDrawDetailResponse;
import com.hotchpotch.lottery.draw.record.LotteryDrawPageResponse;
import com.hotchpotch.lottery.draw.service.LotteryDrawQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 大乐透开奖记录查询接口。
 */
@RestController
@RequestMapping("/api/draws/dlt")
public class LotteryDrawQueryController {

    private final LotteryDrawQueryService queryService;

    /**
     * 初始化大乐透开奖记录查询接口依赖的查询服务。
     */
    public LotteryDrawQueryController(LotteryDrawQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * 查询最新一期大乐透开奖详情。
     */
    @GetMapping("/latest")
    public ApiResponse<LotteryDrawDetailResponse> getLatestDltDraw() {
        return ApiResponse.success(queryService.getLatestDltDraw());
    }

    /**
     * 分页查询大乐透开奖历史摘要列表。
     */
    @GetMapping
    public ApiResponse<LotteryDrawPageResponse> listDltDraws(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(queryService.listDltDraws(pageNo, pageSize));
    }

    /**
     * 按期号查询大乐透开奖详情。
     */
    @GetMapping("/{issueNo}")
    public ApiResponse<LotteryDrawDetailResponse> getDltDrawByIssueNo(@PathVariable String issueNo) {
        return ApiResponse.success(queryService.getDltDrawByIssueNo(issueNo));
    }
}
