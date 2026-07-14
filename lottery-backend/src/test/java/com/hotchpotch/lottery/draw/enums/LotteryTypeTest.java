package com.hotchpotch.lottery.draw.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LotteryTypeTest {

    /**
     * 验证彩票类型编码与数据库和公开 API 使用的字符串保持一致。
     */
    @Test
    void lotteryTypeCodeMatchesPublicContract() {
        assertThat(LotteryType.DLT.code()).isEqualTo("DLT");
    }
}
