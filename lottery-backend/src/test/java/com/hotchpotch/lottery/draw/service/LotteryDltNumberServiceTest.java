package com.hotchpotch.lottery.draw.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.draw.record.LotteryDltNumber;
import java.util.List;
import org.junit.jupiter.api.Test;

class LotteryDltNumberServiceTest {

    private final LotteryDltNumberService service = new LotteryDltNumberService();

    /**
     * 验证合法单注号码会被排序并格式化为两位数字展示。
     */
    @Test
    void parseSingleNormalizesValidNumbers() {
        LotteryDltNumber number = service.parseSingle("35 01 12 05 23 + 11 03");

        assertThat(number.frontNumbers()).containsExactly(1, 5, 12, 23, 35);
        assertThat(number.backNumbers()).containsExactly(3, 11);
        assertThat(number.displayText()).isEqualTo("01 05 12 23 35 + 03 11");
    }

    /**
     * 验证逗号和中文逗号也可以作为号码分隔符。
     */
    @Test
    void parseSingleSupportsCommaSeparators() {
        LotteryDltNumber number = service.parseSingle("01,05，12 23 35 + 03,11");

        assertThat(number.displayText()).isEqualTo("01 05 12 23 35 + 03 11");
    }

    /**
     * 验证缺少加号分隔符时返回明确错误。
     */
    @Test
    void parseSingleRejectsMissingPlusSeparator() {
        assertThatThrownBy(() -> service.parseSingle("01 05 12 23 35 03 11"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请使用 + 分隔前区和后区");
    }

    /**
     * 验证前区号码数量必须为 5 个。
     */
    @Test
    void parseSingleRejectsInvalidFrontCount() {
        assertThatThrownBy(() -> service.parseSingle("01 05 12 23 + 03 11"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("前区号码必须为 5 个");
    }

    /**
     * 验证后区号码数量必须为 2 个。
     */
    @Test
    void parseSingleRejectsInvalidBackCount() {
        assertThatThrownBy(() -> service.parseSingle("01 05 12 23 35 + 03"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("后区号码必须为 2 个");
    }

    /**
     * 验证非数字号码会被拒绝。
     */
    @Test
    void parseSingleRejectsNonNumericToken() {
        assertThatThrownBy(() -> service.parseSingle("01 05 AA 23 35 + 03 11"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("号码必须为数字");
    }

    /**
     * 验证前区号码范围必须在 1 到 35。
     */
    @Test
    void parseSingleRejectsFrontNumberOutOfRange() {
        assertThatThrownBy(() -> service.parseSingle("01 05 12 23 36 + 03 11"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("前区号码范围必须为 01-35");
    }

    /**
     * 验证后区号码范围必须在 1 到 12。
     */
    @Test
    void parseSingleRejectsBackNumberOutOfRange() {
        assertThatThrownBy(() -> service.parseSingle("01 05 12 23 35 + 03 13"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("后区号码范围必须为 01-12");
    }

    /**
     * 验证各区内号码不能重复。
     */
    @Test
    void parseSingleRejectsDuplicatedNumbersWithinSameArea() {
        assertThatThrownBy(() -> service.parseSingle("01 05 12 23 23 + 03 11"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("前区号码不能重复");

        assertThatThrownBy(() -> service.parseSingle("01 05 12 23 35 + 03 03"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("后区号码不能重复");
    }

    /**
     * 验证批量解析会在错误信息中包含行号。
     */
    @Test
    void parseBatchAddsLineNumberToValidationError() {
        assertThatThrownBy(() -> service.parseBatch(List.of(
                "01 05 12 23 35 + 03 11",
                "01 05 12 23 35 + 03")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("第2行：后区号码必须为 2 个");
    }

    /**
     * 验证批量解析最多支持 50 注号码。
     */
    @Test
    void parseBatchRejectsMoreThanFiftyLines() {
        List<String> inputs = java.util.stream.IntStream.rangeClosed(1, 51)
                .mapToObj(index -> "01 05 12 23 35 + 03 11")
                .toList();

        assertThatThrownBy(() -> service.parseBatch(inputs))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("单次最多分析 50 注号码");
    }
}
