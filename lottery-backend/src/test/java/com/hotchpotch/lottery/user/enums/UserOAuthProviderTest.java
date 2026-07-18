package com.hotchpotch.lottery.user.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserOAuthProviderTest {

    /**
     * 验证第三方登录平台编码与数据库存储使用的字符串保持一致。
     */
    @Test
    void oauthProviderCodeMatchesDatabaseContract() {
        assertThat(UserOAuthProvider.WECHAT_MP.code()).isEqualTo("WECHAT_MP");
        assertThat(UserOAuthProvider.WECHAT_WEB.code()).isEqualTo("WECHAT_WEB");
        assertThat(UserOAuthProvider.WECHAT_MINI_PROGRAM.code()).isEqualTo("WECHAT_MINI_PROGRAM");
    }
}
