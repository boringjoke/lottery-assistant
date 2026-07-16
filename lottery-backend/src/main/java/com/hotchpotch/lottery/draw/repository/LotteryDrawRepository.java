package com.hotchpotch.lottery.draw.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hotchpotch.lottery.draw.entity.LotteryDraw;
import com.hotchpotch.lottery.draw.mapper.LotteryDrawMapper;
import java.time.LocalDate;
import java.util.List;
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
     * 按彩票类型查询最新一期开奖记录。
     */
    public Optional<LotteryDraw> findLatestByLotteryType(String lotteryType) {
        LotteryDraw draw = lotteryDrawMapper.selectOne(Wrappers.<LotteryDraw>lambdaQuery()
                .eq(LotteryDraw::getLotteryType, lotteryType)
                .orderByDesc(LotteryDraw::getDrawDate)
                .orderByDesc(LotteryDraw::getIssueNo)
                .last("LIMIT 1"));

        return Optional.ofNullable(draw);
    }

    /**
     * 按彩票类型分页查询开奖记录，按开奖日期和期号倒序返回。
     */
    public List<LotteryDraw> findPageByLotteryType(String lotteryType, int pageNo, int pageSize) {
        return findPageByQuery(lotteryType, null, null, null, pageNo, pageSize);
    }

    /**
     * 按彩票类型、期号和开奖日期范围分页查询开奖记录，按开奖日期和期号倒序返回。
     */
    public List<LotteryDraw> findPageByQuery(
            String lotteryType,
            String issueNo,
            LocalDate startDate,
            LocalDate endDate,
            int pageNo,
            int pageSize) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePageNo - 1) * safePageSize;

        return lotteryDrawMapper.selectList(basePageQuery(lotteryType, issueNo, startDate, endDate)
                .orderByDesc(LotteryDraw::getDrawDate)
                .orderByDesc(LotteryDraw::getIssueNo)
                .last("LIMIT " + safePageSize + " OFFSET " + offset));
    }

    /**
     * 按彩票类型查询全部开奖记录，按开奖日期和期号倒序返回。
     */
    public List<LotteryDraw> findAllByLotteryType(String lotteryType) {
        return lotteryDrawMapper.selectList(Wrappers.<LotteryDraw>lambdaQuery()
                .eq(LotteryDraw::getLotteryType, lotteryType)
                .orderByDesc(LotteryDraw::getDrawDate)
                .orderByDesc(LotteryDraw::getIssueNo));
    }

    /**
     * 按彩票类型统计开奖记录总数。
     */
    public Long countByLotteryType(String lotteryType) {
        return countByQuery(lotteryType, null, null, null);
    }

    /**
     * 按彩票类型、期号和开奖日期范围统计开奖记录总数。
     */
    public Long countByQuery(String lotteryType, String issueNo, LocalDate startDate, LocalDate endDate) {
        return lotteryDrawMapper.selectCount(basePageQuery(lotteryType, issueNo, startDate, endDate));
    }

    /**
     * 构建历史开奖记录分页和统计共用查询条件。
     */
    private LambdaQueryWrapper<LotteryDraw> basePageQuery(
            String lotteryType,
            String issueNo,
            LocalDate startDate,
            LocalDate endDate) {
        LambdaQueryWrapper<LotteryDraw> queryWrapper = Wrappers.<LotteryDraw>lambdaQuery()
                .eq(LotteryDraw::getLotteryType, lotteryType);
        if (issueNo != null && !issueNo.isBlank()) {
            queryWrapper.eq(LotteryDraw::getIssueNo, issueNo.trim());
        }
        if (startDate != null) {
            queryWrapper.ge(LotteryDraw::getDrawDate, startDate);
        }
        if (endDate != null) {
            queryWrapper.le(LotteryDraw::getDrawDate, endDate);
        }

        return queryWrapper;
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
