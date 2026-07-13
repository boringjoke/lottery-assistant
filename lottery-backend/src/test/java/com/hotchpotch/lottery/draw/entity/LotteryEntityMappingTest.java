package com.hotchpotch.lottery.draw.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class LotteryEntityMappingTest {

    @Test
    void lotteryDrawMapsToDrawTableWithAutoIncrementId() throws NoSuchFieldException {
        assertThat(tableNameOf(LotteryDraw.class)).isEqualTo("lottery_draws");
        assertThat(idTypeOf(LotteryDraw.class)).isEqualTo(IdType.AUTO);
        assertThat(fieldNamesOf(LotteryDraw.class))
                .contains(
                        "lotteryType",
                        "issueNo",
                        "drawDate",
                        "frontNumbers",
                        "backNumbers",
                        "poolBalance",
                        "salesAmount",
                        "fetchedTime",
                        "createTime",
                        "updateTime")
                .doesNotContain("poolBalanceYuan", "salesAmountYuan");
    }

    @Test
    void lotteryPrizeTierMapsToPrizeTierTableWithStakeAmount() throws NoSuchFieldException {
        assertThat(tableNameOf(LotteryPrizeTier.class)).isEqualTo("lottery_prize_tiers");
        assertThat(idTypeOf(LotteryPrizeTier.class)).isEqualTo(IdType.AUTO);
        assertThat(fieldNamesOf(LotteryPrizeTier.class))
                .contains(
                        "drawId",
                        "lotteryType",
                        "issueNo",
                        "prizeName",
                        "stakeCount",
                        "stakeAmount",
                        "totalPrizeAmount",
                        "createTime",
                        "updateTime")
                .doesNotContain("stakeAmountYuan", "totalPrizeAmountYuan");
    }

    @Test
    void lotterySyncTaskMapsToSyncTaskTableWithTimeFields() throws NoSuchFieldException {
        assertThat(tableNameOf(LotterySyncTask.class)).isEqualTo("lottery_sync_tasks");
        assertThat(idTypeOf(LotterySyncTask.class)).isEqualTo(IdType.AUTO);
        assertThat(fieldNamesOf(LotterySyncTask.class))
                .contains(
                        "taskNo",
                        "lotteryType",
                        "syncType",
                        "triggerSource",
                        "status",
                        "requestParams",
                        "startPage",
                        "currentPage",
                        "lastSuccessPage",
                        "failedPage",
                        "pageSize",
                        "maxPages",
                        "pageDelayMillis",
                        "stopWhenLastPage",
                        "startTime",
                        "finishTime",
                        "createTime",
                        "updateTime");
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
