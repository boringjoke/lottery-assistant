package com.hotchpotch.lottery.draw.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.draw.entity.LotteryDraw;
import com.hotchpotch.lottery.draw.entity.LotteryPrizeTier;
import com.hotchpotch.lottery.draw.record.LotteryDrawDetailResponse;
import com.hotchpotch.lottery.draw.record.LotteryDrawPageResponse;
import com.hotchpotch.lottery.draw.repository.LotteryDrawRepository;
import com.hotchpotch.lottery.draw.repository.LotteryPrizeTierRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LotteryDrawQueryServiceTest {

    /**
     * 验证查询最新一期时会返回开奖主表和奖级明细。
     */
    @Test
    void getLatestDltDrawReturnsDrawDetailWithPrizeTiers() {
        LotteryDrawRepository drawRepository = mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = mock(LotteryPrizeTierRepository.class);
        LotteryDrawQueryService service = new LotteryDrawQueryService(drawRepository, prizeTierRepository);
        LotteryDraw draw = sampleDraw();
        LotteryPrizeTier prizeTier = samplePrizeTier();

        when(drawRepository.findLatestByLotteryType("DLT")).thenReturn(Optional.of(draw));
        when(prizeTierRepository.findByDrawId(88L)).thenReturn(List.of(prizeTier));

        LotteryDrawDetailResponse response = service.getLatestDltDraw();

        assertThat(response.lotteryType()).isEqualTo("DLT");
        assertThat(response.issueNo()).isEqualTo("26076");
        assertThat(response.frontNumbers()).isEqualTo("01,02,03,04,05");
        assertThat(response.prizeTiers()).hasSize(1);
        assertThat(response.prizeTiers().get(0).prizeName()).isEqualTo("一等奖");
        assertThat(response.prizeTiers().get(0).stakeAmount()).isEqualByComparingTo("10000.00");
    }

    /**
     * 验证按期号查询不存在时会抛出资源不存在业务异常。
     */
    @Test
    void getDltDrawByIssueNoThrowsNotFoundWhenIssueMissing() {
        LotteryDrawRepository drawRepository = mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = mock(LotteryPrizeTierRepository.class);
        LotteryDrawQueryService service = new LotteryDrawQueryService(drawRepository, prizeTierRepository);

        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDltDrawByIssueNo("26000"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
                    assertThat(exception.getMessage()).contains("26000");
                });
    }

    /**
     * 验证分页查询会返回摘要列表和分页元信息。
     */
    @Test
    void listDltDrawsReturnsPageResponse() {
        LotteryDrawRepository drawRepository = mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = mock(LotteryPrizeTierRepository.class);
        LotteryDrawQueryService service = new LotteryDrawQueryService(drawRepository, prizeTierRepository);

        when(drawRepository.findPageByLotteryType("DLT", 2, 10)).thenReturn(List.of(sampleDraw()));
        when(drawRepository.countByLotteryType("DLT")).thenReturn(11L);

        LotteryDrawPageResponse response = service.listDltDraws(2, 10);

        assertThat(response.pageNo()).isEqualTo(2);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.total()).isEqualTo(11L);
        assertThat(response.pages()).isEqualTo(2);
        assertThat(response.draws()).hasSize(1);
        assertThat(response.draws().get(0).issueNo()).isEqualTo("26076");
    }

    private LotteryDraw sampleDraw() {
        LotteryDraw draw = new LotteryDraw();
        draw.setId(88L);
        draw.setLotteryType("DLT");
        draw.setIssueNo("26076");
        draw.setDrawDate(LocalDate.of(2026, 7, 11));
        draw.setFrontNumbers("01,02,03,04,05");
        draw.setBackNumbers("06,07");
        draw.setPoolBalance(new BigDecimal("1000000.00"));
        draw.setSalesAmount(new BigDecimal("500000.00"));
        draw.setSourceUrl("https://www.sporttery.cn/");
        draw.setPdfUrl("https://www.sporttery.cn/dlt.pdf");
        return draw;
    }

    private LotteryPrizeTier samplePrizeTier() {
        LotteryPrizeTier prizeTier = new LotteryPrizeTier();
        prizeTier.setPrizeName("一等奖");
        prizeTier.setStakeCount(2);
        prizeTier.setStakeAmount(new BigDecimal("10000.00"));
        prizeTier.setTotalPrizeAmount(new BigDecimal("20000.00"));
        prizeTier.setSortOrder(1);
        prizeTier.setPrizeGroup("1");
        return prizeTier;
    }
}
