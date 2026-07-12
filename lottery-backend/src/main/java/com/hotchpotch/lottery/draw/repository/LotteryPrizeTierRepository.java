package com.hotchpotch.lottery.draw.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hotchpotch.lottery.draw.entity.LotteryPrizeTier;
import com.hotchpotch.lottery.draw.mapper.LotteryPrizeTierMapper;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 开奖奖级明细 Repository。
 */
@Repository
public class LotteryPrizeTierRepository {

    private final LotteryPrizeTierMapper lotteryPrizeTierMapper;

    public LotteryPrizeTierRepository(LotteryPrizeTierMapper lotteryPrizeTierMapper) {
        this.lotteryPrizeTierMapper = lotteryPrizeTierMapper;
    }

    /**
     * 按开奖主表 ID 查询奖级明细。
     */
    public List<LotteryPrizeTier> findByDrawId(Long drawId) {
        return lotteryPrizeTierMapper.selectList(Wrappers.<LotteryPrizeTier>lambdaQuery()
                .eq(LotteryPrizeTier::getDrawId, drawId)
                .orderByAsc(LotteryPrizeTier::getSortOrder)
                .orderByAsc(LotteryPrizeTier::getId));
    }

    /**
     * 批量插入奖级明细，返回成功插入的记录数。
     */
    public int insertBatch(Collection<LotteryPrizeTier> prizeTiers) {
        int insertedCount = 0;

        for (LotteryPrizeTier prizeTier : prizeTiers) {
            insertedCount += lotteryPrizeTierMapper.insert(prizeTier);
        }

        return insertedCount;
    }
}
