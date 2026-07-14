package com.hotchpotch.lottery.draw.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LotterySyncTaskStatusTest {

    /**
     * 验证同步任务状态编码与数据库和公开 API 使用的字符串保持一致。
     */
    @Test
    void statusCodeMatchesPublicContract() {
        assertThat(LotterySyncTaskStatus.PENDING.code()).isEqualTo("PENDING");
        assertThat(LotterySyncTaskStatus.RUNNING.code()).isEqualTo("RUNNING");
        assertThat(LotterySyncTaskStatus.SUCCESS.code()).isEqualTo("SUCCESS");
        assertThat(LotterySyncTaskStatus.PARTIAL_SUCCESS.code()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(LotterySyncTaskStatus.FAILED.code()).isEqualTo("FAILED");
        assertThat(LotterySyncTaskStatus.RETRIED.code()).isEqualTo("RETRIED");
    }
}
