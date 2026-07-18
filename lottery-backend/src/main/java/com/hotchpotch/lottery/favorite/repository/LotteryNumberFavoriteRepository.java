package com.hotchpotch.lottery.favorite.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hotchpotch.lottery.favorite.entity.LotteryNumberFavorite;
import com.hotchpotch.lottery.favorite.mapper.LotteryNumberFavoriteMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 用户收藏号码 Repository。
 */
@Repository
public class LotteryNumberFavoriteRepository {

    private final LotteryNumberFavoriteMapper lotteryNumberFavoriteMapper;

    public LotteryNumberFavoriteRepository(LotteryNumberFavoriteMapper lotteryNumberFavoriteMapper) {
        this.lotteryNumberFavoriteMapper = lotteryNumberFavoriteMapper;
    }

    /**
     * 按主键查询收藏号码。
     */
    public Optional<LotteryNumberFavorite> findById(Long id) {
        LotteryNumberFavorite favorite = lotteryNumberFavoriteMapper.selectById(id);

        return Optional.ofNullable(favorite);
    }

    /**
     * 按用户、票种和规范化号码查询收藏，用于新增收藏时做幂等判断。
     */
    public Optional<LotteryNumberFavorite> findByUserAndNumbers(
            Long userId,
            String lotteryType,
            String frontNumbers,
            String backNumbers) {
        LotteryNumberFavorite favorite = lotteryNumberFavoriteMapper.selectOne(Wrappers.<LotteryNumberFavorite>lambdaQuery()
                .eq(LotteryNumberFavorite::getUserId, userId)
                .eq(LotteryNumberFavorite::getLotteryType, lotteryType)
                .eq(LotteryNumberFavorite::getFrontNumbers, frontNumbers)
                .eq(LotteryNumberFavorite::getBackNumbers, backNumbers));

        return Optional.ofNullable(favorite);
    }

    /**
     * 按用户和状态分页查询收藏号码，按当前生效时间倒序返回。
     */
    public List<LotteryNumberFavorite> findPageByUserIdAndStatus(
            Long userId,
            String status,
            int pageNo,
            int pageSize) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePageNo - 1) * safePageSize;

        return lotteryNumberFavoriteMapper.selectList(baseUserStatusQuery(userId, status)
                .orderByDesc(LotteryNumberFavorite::getEffectiveTime)
                .orderByDesc(LotteryNumberFavorite::getId)
                .last("LIMIT " + safePageSize + " OFFSET " + offset));
    }

    /**
     * 按用户和状态统计收藏号码数量。
     */
    public Long countByUserIdAndStatus(Long userId, String status) {
        return lotteryNumberFavoriteMapper.selectCount(baseUserStatusQuery(userId, status));
    }

    /**
     * 插入收藏号码。
     */
    public int insert(LotteryNumberFavorite favorite) {
        return lotteryNumberFavoriteMapper.insert(favorite);
    }

    /**
     * 按主键更新收藏号码。
     */
    public int updateById(LotteryNumberFavorite favorite) {
        return lotteryNumberFavoriteMapper.updateById(favorite);
    }

    /**
     * 构建用户收藏分页和统计共用查询条件。
     */
    private LambdaQueryWrapper<LotteryNumberFavorite> baseUserStatusQuery(Long userId, String status) {
        LambdaQueryWrapper<LotteryNumberFavorite> queryWrapper = Wrappers.<LotteryNumberFavorite>lambdaQuery()
                .eq(LotteryNumberFavorite::getUserId, userId);
        if (status != null && !status.isBlank()) {
            queryWrapper.eq(LotteryNumberFavorite::getStatus, status.trim());
        }

        return queryWrapper;
    }
}
