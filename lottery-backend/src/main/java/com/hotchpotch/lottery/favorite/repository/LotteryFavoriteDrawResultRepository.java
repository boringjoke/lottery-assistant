package com.hotchpotch.lottery.favorite.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hotchpotch.lottery.favorite.entity.LotteryFavoriteDrawResult;
import com.hotchpotch.lottery.favorite.mapper.LotteryFavoriteDrawResultMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 收藏号码开奖结果 Repository。
 */
@Repository
public class LotteryFavoriteDrawResultRepository {

    private final LotteryFavoriteDrawResultMapper lotteryFavoriteDrawResultMapper;

    public LotteryFavoriteDrawResultRepository(LotteryFavoriteDrawResultMapper lotteryFavoriteDrawResultMapper) {
        this.lotteryFavoriteDrawResultMapper = lotteryFavoriteDrawResultMapper;
    }

    /**
     * 按主键查询收藏开奖结果。
     */
    public Optional<LotteryFavoriteDrawResult> findById(Long id) {
        LotteryFavoriteDrawResult result = lotteryFavoriteDrawResultMapper.selectById(id);

        return Optional.ofNullable(result);
    }

    /**
     * 按收藏和开奖查询结果，用于生成时做幂等判断。
     */
    public Optional<LotteryFavoriteDrawResult> findByFavoriteIdAndDrawId(Long favoriteId, Long drawId) {
        LotteryFavoriteDrawResult result = lotteryFavoriteDrawResultMapper.selectOne(
                Wrappers.<LotteryFavoriteDrawResult>lambdaQuery()
                        .eq(LotteryFavoriteDrawResult::getFavoriteId, favoriteId)
                        .eq(LotteryFavoriteDrawResult::getDrawId, drawId));

        return Optional.ofNullable(result);
    }

    /**
     * 按用户和收藏分页查询开奖结果，按开奖日期和期号倒序返回。
     */
    public List<LotteryFavoriteDrawResult> findPageByUserIdAndFavoriteId(
            Long userId,
            Long favoriteId,
            int pageNo,
            int pageSize) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePageNo - 1) * safePageSize;

        return lotteryFavoriteDrawResultMapper.selectList(baseUserQuery(userId, favoriteId)
                .orderByDesc(LotteryFavoriteDrawResult::getDrawDate)
                .orderByDesc(LotteryFavoriteDrawResult::getIssueNo)
                .orderByDesc(LotteryFavoriteDrawResult::getId)
                .last("LIMIT " + safePageSize + " OFFSET " + offset));
    }

    /**
     * 按用户和收藏统计开奖结果数量。
     */
    public Long countByUserIdAndFavoriteId(Long userId, Long favoriteId) {
        return lotteryFavoriteDrawResultMapper.selectCount(baseUserQuery(userId, favoriteId));
    }

    /**
     * 插入收藏开奖结果。
     */
    public int insert(LotteryFavoriteDrawResult result) {
        return lotteryFavoriteDrawResultMapper.insert(result);
    }

    /**
     * 按主键更新收藏开奖结果。
     */
    public int updateById(LotteryFavoriteDrawResult result) {
        return lotteryFavoriteDrawResultMapper.updateById(result);
    }

    /**
     * 构建当前用户收藏历史分页和统计共用查询条件。
     */
    private LambdaQueryWrapper<LotteryFavoriteDrawResult> baseUserQuery(Long userId, Long favoriteId) {
        LambdaQueryWrapper<LotteryFavoriteDrawResult> queryWrapper = Wrappers.<LotteryFavoriteDrawResult>lambdaQuery()
                .eq(LotteryFavoriteDrawResult::getUserId, userId);
        if (favoriteId != null) {
            queryWrapper.eq(LotteryFavoriteDrawResult::getFavoriteId, favoriteId);
        }

        return queryWrapper;
    }
}
