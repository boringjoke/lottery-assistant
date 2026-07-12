package com.hotchpotch.lottery.draw.service;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.draw.entity.LotteryDraw;
import com.hotchpotch.lottery.draw.entity.LotteryPrizeTier;
import com.hotchpotch.lottery.draw.record.LotteryDrawDetailResponse;
import com.hotchpotch.lottery.draw.record.LotteryDrawPageResponse;
import com.hotchpotch.lottery.draw.record.LotteryDrawSummaryResponse;
import com.hotchpotch.lottery.draw.record.LotteryPrizeTierResponse;
import com.hotchpotch.lottery.draw.repository.LotteryDrawRepository;
import com.hotchpotch.lottery.draw.repository.LotteryPrizeTierRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 大乐透开奖记录查询服务。
 */
@Service
public class LotteryDrawQueryService {

    private static final String LOTTERY_TYPE_DLT = "DLT";
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final LotteryDrawRepository drawRepository;
    private final LotteryPrizeTierRepository prizeTierRepository;

    /**
     * 初始化开奖记录查询服务依赖的仓储。
     */
    public LotteryDrawQueryService(
            LotteryDrawRepository drawRepository,
            LotteryPrizeTierRepository prizeTierRepository) {
        this.drawRepository = drawRepository;
        this.prizeTierRepository = prizeTierRepository;
    }

    /**
     * 查询最新一期大乐透开奖详情。
     */
    public LotteryDrawDetailResponse getLatestDltDraw() {
        LotteryDraw draw = drawRepository.findLatestByLotteryType(LOTTERY_TYPE_DLT)
                .orElseThrow(() -> notFound("最新一期大乐透开奖不存在"));

        return toDetailResponse(draw, prizeTierRepository.findByDrawId(draw.getId()));
    }

    /**
     * 按期号查询大乐透开奖详情。
     */
    public LotteryDrawDetailResponse getDltDrawByIssueNo(String issueNo) {
        LotteryDraw draw = drawRepository.findByLotteryTypeAndIssueNo(LOTTERY_TYPE_DLT, issueNo)
                .orElseThrow(() -> notFound("大乐透开奖期号不存在: " + issueNo));

        return toDetailResponse(draw, prizeTierRepository.findByDrawId(draw.getId()));
    }

    /**
     * 分页查询大乐透开奖历史摘要列表。
     */
    public LotteryDrawPageResponse listDltDraws(int pageNo, int pageSize) {
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        Long total = drawRepository.countByLotteryType(LOTTERY_TYPE_DLT);
        List<LotteryDrawSummaryResponse> draws = drawRepository
                .findPageByLotteryType(LOTTERY_TYPE_DLT, safePageNo, safePageSize)
                .stream()
                .map(this::toSummaryResponse)
                .toList();

        return new LotteryDrawPageResponse(
                safePageNo,
                safePageSize,
                total,
                calculatePages(total, safePageSize),
                draws);
    }

    /**
     * 将开奖主表和奖级明细转换为详情响应。
     */
    private LotteryDrawDetailResponse toDetailResponse(LotteryDraw draw, List<LotteryPrizeTier> prizeTiers) {
        return new LotteryDrawDetailResponse(
                draw.getLotteryType(),
                draw.getIssueNo(),
                draw.getDrawDate(),
                draw.getFrontNumbers(),
                draw.getBackNumbers(),
                draw.getPoolBalance(),
                draw.getSalesAmount(),
                draw.getSourceUrl(),
                draw.getPdfUrl(),
                prizeTiers.stream().map(this::toPrizeTierResponse).toList());
    }

    /**
     * 将开奖主表转换为历史列表摘要响应。
     */
    private LotteryDrawSummaryResponse toSummaryResponse(LotteryDraw draw) {
        return new LotteryDrawSummaryResponse(
                draw.getLotteryType(),
                draw.getIssueNo(),
                draw.getDrawDate(),
                draw.getFrontNumbers(),
                draw.getBackNumbers(),
                draw.getPoolBalance(),
                draw.getSalesAmount());
    }

    /**
     * 将奖级明细实体转换为奖级响应数据。
     */
    private LotteryPrizeTierResponse toPrizeTierResponse(LotteryPrizeTier prizeTier) {
        return new LotteryPrizeTierResponse(
                prizeTier.getPrizeName(),
                prizeTier.getStakeCount(),
                prizeTier.getStakeAmount(),
                prizeTier.getTotalPrizeAmount(),
                prizeTier.getSortOrder(),
                prizeTier.getPrizeGroup());
    }

    /**
     * 将页码归一化为从 1 开始的有效页码。
     */
    private int normalizePageNo(int pageNo) {
        return pageNo <= 0 ? DEFAULT_PAGE_NO : pageNo;
    }

    /**
     * 将分页大小归一化到允许范围内。
     */
    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 根据总数和分页大小计算总页数。
     */
    private int calculatePages(Long total, int pageSize) {
        if (total == null || total <= 0) {
            return 0;
        }

        return (int) ((total + pageSize - 1) / pageSize);
    }

    /**
     * 创建资源不存在业务异常。
     */
    private BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
