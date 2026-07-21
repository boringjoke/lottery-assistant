package com.hotchpotch.lottery.notification.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationEnumsTest {

    @Test
    void notificationEnumsReturnDatabaseCodes() {
        assertThat(NotificationType.FAVORITE_WINNING.code()).isEqualTo("FAVORITE_WINNING");
        assertThat(NotificationBusinessType.LOTTERY_FAVORITE_WINNING.code()).isEqualTo("LOTTERY_FAVORITE_WINNING");
        assertThat(NotificationReadStatus.UNREAD.code()).isEqualTo("UNREAD");
        assertThat(NotificationReadStatus.READ.code()).isEqualTo("READ");
    }
}
