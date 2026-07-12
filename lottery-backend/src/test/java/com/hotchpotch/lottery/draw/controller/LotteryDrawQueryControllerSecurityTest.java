package com.hotchpotch.lottery.draw.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.config.SecurityConfig;
import com.hotchpotch.lottery.draw.record.LotteryDrawDetailResponse;
import com.hotchpotch.lottery.draw.service.LotteryDrawQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LotteryDrawQueryController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class LotteryDrawQueryControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LotteryDrawQueryService queryService;

    /**
     * 验证公开开奖查询接口不需要 Basic Auth 也可以通过安全过滤链。
     */
    @Test
    void getLatestDltDrawAllowsAnonymousAccess() throws Exception {
        when(queryService.getLatestDltDraw()).thenReturn(new LotteryDrawDetailResponse(
                "DLT",
                "26076",
                LocalDate.of(2026, 7, 11),
                "01,02,03,04,05",
                "06,07",
                new BigDecimal("1000000.00"),
                new BigDecimal("500000.00"),
                "https://www.sporttery.cn/",
                null,
                List.of()));

        mockMvc.perform(get("/api/draws/dlt/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.issueNo").value("26076"));
    }
}
