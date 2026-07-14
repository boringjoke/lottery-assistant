package com.hotchpotch.lottery.draw.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LotterySyncTypeTest {

    /**
     * 验证同步类型编码与数据库和公开 API 使用的字符串保持一致。
     */
    @Test
    void syncTypeCodeMatchesPublicContract() {
        assertThat(LotterySyncType.LATEST.code()).isEqualTo("LATEST");
        assertThat(LotterySyncType.HISTORY_PAGE.code()).isEqualTo("HISTORY_PAGE");
        assertThat(LotterySyncType.HISTORY.code()).isEqualTo("HISTORY");
        assertThat(LotterySyncType.ISSUE_RANGE.code()).isEqualTo("ISSUE_RANGE");
        assertThat(LotterySyncType.DATE_RANGE.code()).isEqualTo("DATE_RANGE");
    }
}
