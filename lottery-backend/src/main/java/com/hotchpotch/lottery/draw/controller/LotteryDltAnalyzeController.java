package com.hotchpotch.lottery.draw.controller;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.common.response.ApiResponse;
import com.hotchpotch.lottery.draw.record.LotteryDltAnalyzeRequest;
import com.hotchpotch.lottery.draw.record.LotteryDltAnalyzeResponse;
import com.hotchpotch.lottery.draw.service.LotteryDltAnalyzeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 大乐透号码分析接口。
 */
@RestController
@RequestMapping("/api/lottery/dlt")
public class LotteryDltAnalyzeController {

    private final LotteryDltAnalyzeService analyzeService;

    /**
     * 初始化大乐透号码分析接口依赖的分析服务。
     */
    public LotteryDltAnalyzeController(LotteryDltAnalyzeService analyzeService) {
        this.analyzeService = analyzeService;
    }

    /**
     * 分析一注或多注大乐透号码的历史命中情况。
     */
    @PostMapping("/analyze")
    public ApiResponse<LotteryDltAnalyzeResponse> analyzeDltNumbers(
            @RequestBody(required = false) LotteryDltAnalyzeRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "号码分析请求体不能为空");
        }

        return ApiResponse.success(analyzeService.analyze(request));
    }
}
