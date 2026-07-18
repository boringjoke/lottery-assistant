package com.hotchpotch.lottery.favorite.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class LotteryNumberFavoriteEntityMappingTest {

    @Test
    void lotteryNumberFavoriteMapsToFavoriteTableWithAutoIncrementId() throws NoSuchFieldException {
        assertThat(tableNameOf(LotteryNumberFavorite.class)).isEqualTo("lottery_number_favorites");
        assertThat(idTypeOf(LotteryNumberFavorite.class)).isEqualTo(IdType.AUTO);
        assertThat(fieldNamesOf(LotteryNumberFavorite.class))
                .contains(
                        "userId",
                        "lotteryType",
                        "frontNumbers",
                        "backNumbers",
                        "favoriteName",
                        "remark",
                        "status",
                        "favoriteTime",
                        "effectiveTime",
                        "cancelTime",
                        "createTime",
                        "updateTime")
                .doesNotContain("user", "drawId");
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
