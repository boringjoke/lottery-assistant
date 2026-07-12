package com.hotchpotch.lottery.draw.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hotchpotch.lottery.draw.entity.LotteryDraw;
import com.hotchpotch.lottery.draw.mapper.LotteryDrawMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 彩票开奖主表 Repository。
 */
@Repository
public class LotteryDrawRepository {

    private final LotteryDrawMapper lotteryDrawMapper;

    public LotteryDrawRepository(LotteryDrawMapper lotteryDrawMapper) {
        this.lotteryDrawMapper = lotteryDrawMapper;
    }

    /**
     * 按彩票类型和期号查询开奖记录，用于同步入库时做幂等判断。
     */
    public Optional<LotteryDraw> findByLotteryTypeAndIssueNo(String lotteryType, String issueNo) {
        LotteryDraw draw = lotteryDrawMapper.selectOne(Wrappers.<LotteryDraw>lambdaQuery()
                .eq(LotteryDraw::getLotteryType, lotteryType)
                .eq(LotteryDraw::getIssueNo, issueNo));

        return Optional.ofNullable(draw);
    }

    /**
     * 插入开奖记录。
     */
    public int insert(LotteryDraw draw) {
        return lotteryDrawMapper.insert(draw);
    }

    /**
     * 按主键更新开奖记录。
     */
    public int updateById(LotteryDraw draw) {
        return lotteryDrawMapper.updateById(draw);
    }
}
