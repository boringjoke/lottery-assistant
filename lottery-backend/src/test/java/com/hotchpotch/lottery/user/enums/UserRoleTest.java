package com.hotchpotch.lottery.user.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserRoleTest {

    /**
     * 验证用户角色编码与数据库和权限判断使用的字符串保持一致。
     */
    @Test
    void roleCodeMatchesDatabaseContract() {
        assertThat(UserRole.USER.code()).isEqualTo("USER");
        assertThat(UserRole.ADMIN.code()).isEqualTo("ADMIN");
    }
}
