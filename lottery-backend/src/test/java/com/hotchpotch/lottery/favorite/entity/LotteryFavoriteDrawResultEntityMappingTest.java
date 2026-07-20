package com.hotchpotch.lottery.favorite.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class LotteryFavoriteDrawResultEntityMappingTest {

    @Test
    void lotteryFavoriteDrawResultMapsToResultTableWithAutoIncrementId() throws NoSuchFieldException {
        assertThat(tableNameOf(LotteryFavoriteDrawResult.class)).isEqualTo("lottery_favorite_draw_results");
        assertThat(idTypeOf(LotteryFavoriteDrawResult.class)).isEqualTo(IdType.AUTO);
        assertThat(fieldNamesOf(LotteryFavoriteDrawResult.class))
                .contains(
                        "favoriteId",
                        "userId",
                        "drawId",
                        "lotteryType",
                        "issueNo",
                        "drawDate",
                        "favoriteFrontNumbers",
                        "favoriteBackNumbers",
                        "drawFrontNumbers",
                        "drawBackNumbers",
                        "frontHitCount",
                        "backHitCount",
                        "winning",
                        "prizeLevel",
                        "prizeName",
                        "ruleVersion",
                        "stakeAmount",
                        "calculatedTime",
                        "createTime",
                        "updateTime")
                .doesNotContain("favorite", "draw");
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
