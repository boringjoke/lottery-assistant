package com.hotchpotch.lottery.draw.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.common.response.GlobalExceptionHandler;
import com.hotchpotch.lottery.draw.record.LotteryDrawDetailResponse;
import com.hotchpotch.lottery.draw.record.LotteryDrawPageResponse;
import com.hotchpotch.lottery.draw.record.LotteryDrawSummaryResponse;
import com.hotchpotch.lottery.draw.record.LotteryPrizeTierResponse;
import com.hotchpotch.lottery.draw.service.LotteryDrawQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LotteryDrawQueryControllerTest {

    /**
     * 验证最新期开奖查询接口返回统一成功响应。
     */
    @Test
    void getLatestDltDrawReturnsSuccessResponse() throws Exception {
        LotteryDrawQueryService queryService = mock(LotteryDrawQueryService.class);
        when(queryService.getLatestDltDraw()).thenReturn(sampleDetail());
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new LotteryDrawQueryController(queryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/draws/dlt/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.issueNo").value("26076"))
                .andExpect(jsonPath("$.data.prizeTiers[0].prizeName").value("一等奖"));
    }

    /**
     * 验证按期号查询接口返回指定期开奖详情。
     */
    @Test
    void getDltDrawByIssueNoReturnsSuccessResponse() throws Exception {
        LotteryDrawQueryService queryService = mock(LotteryDrawQueryService.class);
        when(queryService.getDltDrawByIssueNo("26076")).thenReturn(sampleDetail());
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new LotteryDrawQueryController(queryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/draws/dlt/26076"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.issueNo").value("26076"))
                .andExpect(jsonPath("$.data.frontNumbers").value("01,02,03,04,05"));
    }

    /**
     * 验证历史开奖分页接口返回分页摘要列表。
     */
    @Test
    void listDltDrawsReturnsPageResponse() throws Exception {
        LotteryDrawQueryService queryService = mock(LotteryDrawQueryService.class);
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 31);
        when(queryService.listDltDraws(1, 20, "26076", startDate, endDate)).thenReturn(new LotteryDrawPageResponse(
                1,
                20,
                1L,
                1,
                List.of(sampleSummary())));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new LotteryDrawQueryController(queryService))
                .build();

        mockMvc.perform(get("/api/draws/dlt")
                        .param("pageNo", "1")
                        .param("pageSize", "20")
                        .param("issueNo", "26076")
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.draws[0].issueNo").value("26076"));
        verify(queryService).listDltDraws(1, 20, "26076", startDate, endDate);
    }

    private LotteryDrawDetailResponse sampleDetail() {
        return new LotteryDrawDetailResponse(
                "DLT",
                "26076",
                LocalDate.of(2026, 7, 11),
                "01,02,03,04,05",
                "06,07",
                new BigDecimal("1000000.00"),
                new BigDecimal("500000.00"),
                "https://www.sporttery.cn/",
                "https://www.sporttery.cn/dlt.pdf",
                List.of(new LotteryPrizeTierResponse(
                        "一等奖",
                        2,
                        new BigDecimal("10000.00"),
                        new BigDecimal("20000.00"),
                        1,
                        "1")));
    }

    private LotteryDrawSummaryResponse sampleSummary() {
        return new LotteryDrawSummaryResponse(
                "DLT",
                "26076",
                LocalDate.of(2026, 7, 11),
                "01,02,03,04,05",
                "06,07",
                new BigDecimal("1000000.00"),
                new BigDecimal("500000.00"));
    }
}
