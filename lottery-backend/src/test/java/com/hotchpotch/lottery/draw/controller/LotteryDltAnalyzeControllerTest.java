package com.hotchpotch.lottery.draw.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.common.response.GlobalExceptionHandler;
import com.hotchpotch.lottery.draw.record.LotteryDltAnalyzeHitDetail;
import com.hotchpotch.lottery.draw.record.LotteryDltAnalyzeNumberResult;
import com.hotchpotch.lottery.draw.record.LotteryDltAnalyzeRequest;
import com.hotchpotch.lottery.draw.record.LotteryDltAnalyzeResponse;
import com.hotchpotch.lottery.draw.service.LotteryDltAnalyzeService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LotteryDltAnalyzeControllerTest {

    /**
     * 验证大乐透号码分析接口返回统一成功响应。
     */
    @Test
    void analyzeDltNumbersReturnsSuccessResponse() throws Exception {
        LotteryDltAnalyzeService analyzeService = mock(LotteryDltAnalyzeService.class);
        when(analyzeService.analyze(new LotteryDltAnalyzeRequest(List.of("01 05 12 23 35 + 03 11"))))
                .thenReturn(sampleResponse());
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new LotteryDltAnalyzeController(analyzeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/lottery/dlt/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "numbers": [
                                        "01 05 12 23 35 + 03 11"
                                    ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalNumberCount").value(1))
                .andExpect(jsonPath("$.data.analyzedDrawCount").value(1))
                .andExpect(jsonPath("$.data.winningNumberCount").value(1))
                .andExpect(jsonPath("$.data.bestPrizeName").value("一等奖"))
                .andExpect(jsonPath("$.data.results[0].displayText").value("01 05 12 23 35 + 03 11"))
                .andExpect(jsonPath("$.data.results[0].hitDetails[0].issueNo").value("26076"));

        verify(analyzeService).analyze(new LotteryDltAnalyzeRequest(List.of("01 05 12 23 35 + 03 11")));
    }

    /**
     * 验证大乐透号码分析接口会拒绝空请求体。
     */
    @Test
    void analyzeDltNumbersRejectsEmptyBody() throws Exception {
        LotteryDltAnalyzeService analyzeService = mock(LotteryDltAnalyzeService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new LotteryDltAnalyzeController(analyzeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/lottery/dlt/analyze"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("号码分析请求体不能为空"));

        verify(analyzeService, never()).analyze(org.mockito.ArgumentMatchers.any());
    }

    /**
     * 构造号码分析响应样例。
     */
    private LotteryDltAnalyzeResponse sampleResponse() {
        return new LotteryDltAnalyzeResponse(
                1,
                1,
                1,
                1,
                1,
                "一等奖",
                List.of(new LotteryDltAnalyzeNumberResult(
                        1,
                        "01 05 12 23 35 + 03 11",
                        "01 05 12 23 35 + 03 11",
                        List.of(1, 5, 12, 23, 35),
                        List.of(3, 11),
                        true,
                        1,
                        1,
                        "一等奖",
                        List.of(new LotteryDltAnalyzeHitDetail(
                                "26076",
                                LocalDate.of(2026, 7, 11),
                                "01,05,12,23,35",
                                "03,11",
                                5,
                                2,
                                true,
                                1,
                                "一等奖",
                                "DLT_2019")))));
    }
}
