package com.hotchpotch.lottery.draw.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.draw.record.LotteryDltPrizeResult;
import org.junit.jupiter.api.Test;

class LotteryDltPrizeRuleServiceTest {

    private final LotteryDltPrizeRuleService service = new LotteryDltPrizeRuleService();

    /**
     * 验证 5 个前区和 2 个后区命中时判定为一等奖。
     */
    @Test
    void determinePrizeReturnsFirstPrize() {
        assertPrize(service.determinePrize(5, 2), 1, "一等奖", true);
    }

    /**
     * 验证 5 个前区和 1 个后区命中时判定为二等奖。
     */
    @Test
    void determinePrizeReturnsSecondPrize() {
        assertPrize(service.determinePrize(5, 1), 2, "二等奖", true);
    }

    /**
     * 验证 5 个前区命中时判定为三等奖。
     */
    @Test
    void determinePrizeReturnsThirdPrize() {
        assertPrize(service.determinePrize(5, 0), 3, "三等奖", true);
    }

    /**
     * 验证 4 个前区和 2 个后区命中时判定为四等奖。
     */
    @Test
    void determinePrizeReturnsFourthPrize() {
        assertPrize(service.determinePrize(4, 2), 4, "四等奖", true);
    }

    /**
     * 验证 4 个前区和 1 个后区命中时判定为五等奖。
     */
    @Test
    void determinePrizeReturnsFifthPrize() {
        assertPrize(service.determinePrize(4, 1), 5, "五等奖", true);
    }

    /**
     * 验证 3 个前区和 2 个后区命中时判定为六等奖。
     */
    @Test
    void determinePrizeReturnsSixthPrize() {
        assertPrize(service.determinePrize(3, 2), 6, "六等奖", true);
    }

    /**
     * 验证 4 个前区命中时判定为七等奖。
     */
    @Test
    void determinePrizeReturnsSeventhPrize() {
        assertPrize(service.determinePrize(4, 0), 7, "七等奖", true);
    }

    /**
     * 验证 3+1 或 2+2 命中时判定为八等奖。
     */
    @Test
    void determinePrizeReturnsEighthPrize() {
        assertPrize(service.determinePrize(3, 1), 8, "八等奖", true);
        assertPrize(service.determinePrize(2, 2), 8, "八等奖", true);
    }

    /**
     * 验证 3+0、2+1、1+2、0+2 命中时判定为九等奖。
     */
    @Test
    void determinePrizeReturnsNinthPrize() {
        assertPrize(service.determinePrize(3, 0), 9, "九等奖", true);
        assertPrize(service.determinePrize(2, 1), 9, "九等奖", true);
        assertPrize(service.determinePrize(1, 2), 9, "九等奖", true);
        assertPrize(service.determinePrize(0, 2), 9, "九等奖", true);
    }

    /**
     * 验证未达到任一奖级条件时判定为未中奖。
     */
    @Test
    void determinePrizeReturnsNoPrize() {
        LotteryDltPrizeResult result = service.determinePrize(2, 0);

        assertThat(result.winning()).isFalse();
        assertThat(result.prizeLevel()).isNull();
        assertThat(result.prizeName()).isEqualTo("未中奖");
        assertThat(result.ruleVersion()).isEqualTo("DLT_2019");
    }

    /**
     * 验证命中数量超出大乐透范围时返回参数错误。
     */
    @Test
    void determinePrizeRejectsInvalidHitCount() {
        assertThatThrownBy(() -> service.determinePrize(-1, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("前区命中数必须在 0-5 之间");

        assertThatThrownBy(() -> service.determinePrize(0, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("后区命中数必须在 0-2 之间");
    }

    /**
     * 断言奖级判定结果。
     */
    private void assertPrize(
            LotteryDltPrizeResult result,
            Integer prizeLevel,
            String prizeName,
            boolean winning) {
        assertThat(result.prizeLevel()).isEqualTo(prizeLevel);
        assertThat(result.prizeName()).isEqualTo(prizeName);
        assertThat(result.winning()).isEqualTo(winning);
        assertThat(result.ruleVersion()).isEqualTo("DLT_2019");
    }
}
