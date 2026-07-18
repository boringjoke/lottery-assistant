package com.hotchpotch.lottery.favorite.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LotteryNumberFavoriteStatusTest {

    @Test
    void statusCodeUsesEnumNameForDatabaseValue() {
        assertThat(LotteryNumberFavoriteStatus.ACTIVE.code()).isEqualTo("ACTIVE");
        assertThat(LotteryNumberFavoriteStatus.CANCELLED.code()).isEqualTo("CANCELLED");
    }
}
