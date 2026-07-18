package com.hotchpotch.lottery.user.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserCredentialTypeTest {

    /**
     * 验证登录凭证类型编码与数据库存储使用的字符串保持一致。
     */
    @Test
    void credentialTypeCodeMatchesDatabaseContract() {
        assertThat(UserCredentialType.USERNAME.code()).isEqualTo("USERNAME");
        assertThat(UserCredentialType.PHONE.code()).isEqualTo("PHONE");
        assertThat(UserCredentialType.EMAIL.code()).isEqualTo("EMAIL");
    }
}
