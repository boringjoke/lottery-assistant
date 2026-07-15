package com.hotchpotch.lottery.draw.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotchpotch.lottery.config.SecurityConfig;
import com.hotchpotch.lottery.draw.record.LotteryDltAnalyzeRequest;
import com.hotchpotch.lottery.draw.record.LotteryDltAnalyzeResponse;
import com.hotchpotch.lottery.draw.service.LotteryDltAnalyzeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LotteryDltAnalyzeController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class LotteryDltAnalyzeControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LotteryDltAnalyzeService analyzeService;

    /**
     * 验证公开号码分析接口不需要 Basic Auth 也可以通过安全过滤链。
     */
    @Test
    void analyzeDltNumbersAllowsAnonymousAccess() throws Exception {
        when(analyzeService.analyze(new LotteryDltAnalyzeRequest(List.of("01 05 12 23 35 + 03 11"))))
                .thenReturn(new LotteryDltAnalyzeResponse(
                        1,
                        1,
                        0,
                        0,
                        null,
                        "未中奖",
                        List.of()));

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
                .andExpect(jsonPath("$.data.analyzedDrawCount").value(1));
    }
}
