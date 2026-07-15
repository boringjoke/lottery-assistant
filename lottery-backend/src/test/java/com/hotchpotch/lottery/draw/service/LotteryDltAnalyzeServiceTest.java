package com.hotchpotch.lottery.draw.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.draw.entity.LotteryDraw;
import com.hotchpotch.lottery.draw.record.LotteryDltAnalyzeRequest;
import com.hotchpotch.lottery.draw.record.LotteryDltAnalyzeResponse;
import com.hotchpotch.lottery.draw.repository.LotteryDrawRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class LotteryDltAnalyzeServiceTest {

    /**
     * 验证单注号码命中一等奖时会返回中奖汇总和明细。
     */
    @Test
    void analyzeReturnsFirstPrizeHitForSingleNumber() {
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryDltAnalyzeService service = newService(drawRepository);
        when(drawRepository.findAllByLotteryType("DLT")).thenReturn(List.of(
                sampleDraw("26076", "01,05,12,23,35", "03,11")));

        LotteryDltAnalyzeResponse response = service.analyze(new LotteryDltAnalyzeRequest(List.of(
                "01 05 12 23 35 + 03 11")));

        assertThat(response.totalNumberCount()).isEqualTo(1);
        assertThat(response.analyzedDrawCount()).isEqualTo(1);
        assertThat(response.winningNumberCount()).isEqualTo(1);
        assertThat(response.winningHitCount()).isEqualTo(1);
        assertThat(response.bestPrizeLevel()).isEqualTo(1);
        assertThat(response.bestPrizeName()).isEqualTo("一等奖");
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).displayText()).isEqualTo("01 05 12 23 35 + 03 11");
        assertThat(response.results().get(0).winning()).isTrue();
        assertThat(response.results().get(0).bestPrizeName()).isEqualTo("一等奖");
        assertThat(response.results().get(0).hitDetails()).hasSize(1);
        assertThat(response.results().get(0).hitDetails().get(0).issueNo()).isEqualTo("26076");
        assertThat(response.results().get(0).hitDetails().get(0).frontHitCount()).isEqualTo(5);
        assertThat(response.results().get(0).hitDetails().get(0).backHitCount()).isEqualTo(2);
    }

    /**
     * 验证单注号码无中奖记录时仍返回规范化号码和空明细。
     */
    @Test
    void analyzeReturnsNoPrizeForSingleNumber() {
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryDltAnalyzeService service = newService(drawRepository);
        when(drawRepository.findAllByLotteryType("DLT")).thenReturn(List.of(
                sampleDraw("26076", "01,05,12,23,35", "03,11")));

        LotteryDltAnalyzeResponse response = service.analyze(new LotteryDltAnalyzeRequest(List.of(
                "06 07 08 09 10 + 01 02")));

        assertThat(response.totalNumberCount()).isEqualTo(1);
        assertThat(response.analyzedDrawCount()).isEqualTo(1);
        assertThat(response.winningNumberCount()).isZero();
        assertThat(response.winningHitCount()).isZero();
        assertThat(response.bestPrizeLevel()).isNull();
        assertThat(response.bestPrizeName()).isEqualTo("未中奖");
        assertThat(response.results().get(0).winning()).isFalse();
        assertThat(response.results().get(0).hitDetails()).isEmpty();
    }

    /**
     * 验证批量号码会分别计算每注的中奖明细和整体最高奖级。
     */
    @Test
    void analyzeReturnsResultsForBatchNumbers() {
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryDltAnalyzeService service = newService(drawRepository);
        when(drawRepository.findAllByLotteryType("DLT")).thenReturn(List.of(
                sampleDraw("26076", "01,05,12,23,35", "03,11"),
                sampleDraw("26075", "01,05,12,23,30", "03,10")));

        LotteryDltAnalyzeResponse response = service.analyze(new LotteryDltAnalyzeRequest(List.of(
                "01 05 12 23 35 + 03 11",
                "01 05 12 23 30 + 03 09")));

        assertThat(response.totalNumberCount()).isEqualTo(2);
        assertThat(response.analyzedDrawCount()).isEqualTo(2);
        assertThat(response.winningNumberCount()).isEqualTo(2);
        assertThat(response.winningHitCount()).isEqualTo(4);
        assertThat(response.bestPrizeLevel()).isEqualTo(1);
        assertThat(response.results()).hasSize(2);
        assertThat(response.results().get(0).hitDetails()).extracting("prizeName")
                .containsExactly("一等奖", "五等奖");
        assertThat(response.results().get(1).bestPrizeName()).isEqualTo("二等奖");
    }

    /**
     * 验证没有历史开奖数据时返回空命中结果。
     */
    @Test
    void analyzeReturnsEmptyHitsWhenNoDraws() {
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryDltAnalyzeService service = newService(drawRepository);
        when(drawRepository.findAllByLotteryType("DLT")).thenReturn(List.of());

        LotteryDltAnalyzeResponse response = service.analyze(new LotteryDltAnalyzeRequest(List.of(
                "01 05 12 23 35 + 03 11")));

        assertThat(response.totalNumberCount()).isEqualTo(1);
        assertThat(response.analyzedDrawCount()).isZero();
        assertThat(response.winningNumberCount()).isZero();
        assertThat(response.winningHitCount()).isZero();
        assertThat(response.results().get(0).hitDetails()).isEmpty();
    }

    /**
     * 验证批量输入解析错误会透传行号。
     */
    @Test
    void analyzeKeepsLineNumberWhenNumberParsingFails() {
        LotteryDltAnalyzeService service = newService(org.mockito.Mockito.mock(LotteryDrawRepository.class));

        assertThatThrownBy(() -> service.analyze(new LotteryDltAnalyzeRequest(List.of(
                "01 05 12 23 35 + 03 11",
                "01 05 12 23 35 + 03"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("第2行：后区号码必须为 2 个");
    }

    /**
     * 创建大乐透号码分析服务。
     */
    private LotteryDltAnalyzeService newService(LotteryDrawRepository drawRepository) {
        return new LotteryDltAnalyzeService(
                new LotteryDltNumberService(),
                new LotteryDltPrizeRuleService(),
                drawRepository);
    }

    /**
     * 构造一条历史开奖样例。
     */
    private LotteryDraw sampleDraw(String issueNo, String frontNumbers, String backNumbers) {
        LotteryDraw draw = new LotteryDraw();
        draw.setLotteryType("DLT");
        draw.setIssueNo(issueNo);
        draw.setDrawDate(LocalDate.of(2026, 7, 11));
        draw.setFrontNumbers(frontNumbers);
        draw.setBackNumbers(backNumbers);
        return draw;
    }
}
