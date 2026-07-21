package com.hotchpotch.lottery.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class UserNotificationEntityMappingTest {

    @Test
    void userNotificationMapsToNotificationTableWithAutoIncrementId() throws NoSuchFieldException {
        assertThat(tableNameOf(UserNotification.class)).isEqualTo("user_notifications");
        assertThat(idTypeOf(UserNotification.class)).isEqualTo(IdType.AUTO);
        assertThat(fieldNamesOf(UserNotification.class))
                .contains(
                        "userId",
                        "notificationType",
                        "businessType",
                        "businessKey",
                        "title",
                        "content",
                        "readStatus",
                        "readTime",
                        "createTime",
                        "updateTime")
                .doesNotContain("user", "business");
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
