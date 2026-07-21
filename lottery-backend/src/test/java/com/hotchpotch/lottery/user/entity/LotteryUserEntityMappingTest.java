package com.hotchpotch.lottery.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class LotteryUserEntityMappingTest {

    @Test
    void lotteryUserMapsToUserTableWithAutoIncrementId() throws NoSuchFieldException {
        assertThat(tableNameOf(LotteryUser.class)).isEqualTo("lottery_users");
        assertThat(idTypeOf(LotteryUser.class)).isEqualTo(IdType.AUTO);
        assertThat(fieldNamesOf(LotteryUser.class))
                .contains(
                        "nickname",
                        "avatarUrl",
                        "status",
                        "emailNotificationEnabled",
                        "lastLoginTime",
                        "createTime",
                        "updateTime")
                .doesNotContain("username", "password", "passwordHash");
    }

    @Test
    void lotteryUserCredentialMapsToCredentialTableWithAutoIncrementId() throws NoSuchFieldException {
        assertThat(tableNameOf(LotteryUserCredential.class)).isEqualTo("lottery_user_credentials");
        assertThat(idTypeOf(LotteryUserCredential.class)).isEqualTo(IdType.AUTO);
        assertThat(fieldNamesOf(LotteryUserCredential.class))
                .contains(
                        "userId",
                        "credentialType",
                        "identifier",
                        "passwordHash",
                        "verified",
                        "createTime",
                        "updateTime")
                .doesNotContain("phone", "email", "username");
    }

    @Test
    void lotteryUserOAuthAccountMapsToOAuthAccountTableWithAutoIncrementId() throws NoSuchFieldException {
        assertThat(tableNameOf(LotteryUserOAuthAccount.class)).isEqualTo("lottery_user_oauth_accounts");
        assertThat(idTypeOf(LotteryUserOAuthAccount.class)).isEqualTo(IdType.AUTO);
        assertThat(fieldNamesOf(LotteryUserOAuthAccount.class))
                .contains(
                        "userId",
                        "provider",
                        "openId",
                        "unionId",
                        "nickname",
                        "avatarUrl",
                        "createTime",
                        "updateTime");
    }

    @Test
    void lotteryUserRoleMapsToUserRoleTableWithAutoIncrementId() throws NoSuchFieldException {
        assertThat(tableNameOf(LotteryUserRole.class)).isEqualTo("lottery_user_roles");
        assertThat(idTypeOf(LotteryUserRole.class)).isEqualTo(IdType.AUTO);
        assertThat(fieldNamesOf(LotteryUserRole.class))
                .contains("userId", "roleCode", "createTime", "updateTime");
    }

    private String tableNameOf(Class<?> entityType) {
        return entityType.getAnnotation(TableName.class).value();
    }

    private IdType idTypeOf(Class<?> entityType) throws NoSuchFieldException {
        return entityType.getDeclaredField("id").getAnnotation(TableId.class).type();
    }

    private Iterable<String> fieldNamesOf(Class<?> entityType) {
        return Arrays.stream(entityType.getDeclaredFields())
                .map(Field::getName)
                .toList();
    }
}
