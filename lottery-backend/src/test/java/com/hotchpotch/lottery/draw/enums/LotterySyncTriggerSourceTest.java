package com.hotchpotch.lottery.draw.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LotterySyncTriggerSourceTest {

    /**
     * 验证触发来源编码与数据库和公开 API 使用的字符串保持一致。
     */
    @Test
    void triggerSourceCodeMatchesPublicContract() {
        assertThat(LotterySyncTriggerSource.ADMIN.code()).isEqualTo("ADMIN");
        assertThat(LotterySyncTriggerSource.SYSTEM.code()).isEqualTo("SYSTEM");
    }
}
