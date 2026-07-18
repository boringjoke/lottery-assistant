package com.hotchpotch.lottery.user.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserStatusTest {

    /**
     * 验证用户状态编码与数据库存储使用的字符串保持一致。
     */
    @Test
    void statusCodeMatchesDatabaseContract() {
        assertThat(UserStatus.ACTIVE.code()).isEqualTo("ACTIVE");
        assertThat(UserStatus.DISABLED.code()).isEqualTo("DISABLED");
    }
}
