package com.hotchpotch.lottery.favorite.service;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.draw.entity.LotteryDraw;
import com.hotchpotch.lottery.draw.entity.LotteryPrizeTier;
import com.hotchpotch.lottery.draw.enums.LotteryType;
import com.hotchpotch.lottery.draw.record.LotteryDltNumber;
import com.hotchpotch.lottery.draw.record.LotteryDltPrizeResult;
import com.hotchpotch.lottery.draw.repository.LotteryPrizeTierRepository;
import com.hotchpotch.lottery.draw.service.LotteryDltNumberService;
import com.hotchpotch.lottery.draw.service.LotteryDltPrizeRuleService;
import com.hotchpotch.lottery.favorite.entity.LotteryFavoriteDrawResult;
import com.hotchpotch.lottery.favorite.entity.LotteryNumberFavorite;
import com.hotchpotch.lottery.favorite.repository.LotteryFavoriteDrawResultRepository;
import com.hotchpotch.lottery.favorite.repository.LotteryNumberFavoriteRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 收藏号码开奖结果生成服务。
 */
@Service
public class LotteryFavoriteDrawResultGenerateService {

    private static final LocalTime DLT_DRAW_TIME = LocalTime.of(20, 30);

    private final LotteryNumberFavoriteRepository favoriteRepository;
    private final LotteryFavoriteDrawResultRepository resultRepository;
    private final LotteryPrizeTierRepository prizeTierRepository;
    private final LotteryDltNumberService numberService;
    private final LotteryDltPrizeRuleService prizeRuleService;

    public LotteryFavoriteDrawResultGenerateService(
            LotteryNumberFavoriteRepository favoriteRepository,
            LotteryFavoriteDrawResultRepository resultRepository,
            LotteryPrizeTierRepository prizeTierRepository,
            LotteryDltNumberService numberService,
            LotteryDltPrizeRuleService prizeRuleService) {
        this.favoriteRepository = favoriteRepository;
        this.resultRepository = resultRepository;
        this.prizeTierRepository = prizeTierRepository;
        this.numberService = numberService;
        this.prizeRuleService = prizeRuleService;
    }

    /**
     * 为指定已入库开奖生成所有当时有效收藏号码的开奖结果。
     */
    @Transactional
    public int generateForDraw(LotteryDraw draw) {
        validateDraw(draw);
        LocalDateTime drawTime = draw.getDrawDate().atTime(DLT_DRAW_TIME);
        List<LotteryNumberFavorite> favorites = favoriteRepository.findActiveAtDrawTime(
                draw.getLotteryType(),
                drawTime);
        List<LotteryPrizeTier> prizeTiers = prizeTierRepository.findByDrawId(draw.getId());
        LotteryDltNumber drawNumber = parseNumber(draw.getFrontNumbers(), draw.getBackNumbers());
        int generatedCount = 0;

        for (LotteryNumberFavorite favorite : favorites) {
            if (resultRepository.findByFavoriteIdAndDrawId(favorite.getId(), draw.getId()).isPresent()) {
                continue;
            }

            LotteryFavoriteDrawResult result = buildResult(favorite, draw, drawNumber, prizeTiers);
            resultRepository.insert(result);
            generatedCount++;
        }

        return generatedCount;
    }

    /**
     * 构建单条收藏号码开奖结果。
     */
    private LotteryFavoriteDrawResult buildResult(
            LotteryNumberFavorite favorite,
            LotteryDraw draw,
            LotteryDltNumber drawNumber,
            List<LotteryPrizeTier> prizeTiers) {
        LotteryDltNumber favoriteNumber = parseNumber(favorite.getFrontNumbers(), favorite.getBackNumbers());
        int frontHitCount = hitCount(favoriteNumber.frontNumbers(), drawNumber.frontNumbers());
        int backHitCount = hitCount(favoriteNumber.backNumbers(), drawNumber.backNumbers());
        LotteryDltPrizeResult prizeResult = prizeRuleService.determinePrize(frontHitCount, backHitCount);

        LotteryFavoriteDrawResult result = new LotteryFavoriteDrawResult();
        result.setFavoriteId(favorite.getId());
        result.setUserId(favorite.getUserId());
        result.setDrawId(draw.getId());
        result.setLotteryType(draw.getLotteryType());
        result.setIssueNo(draw.getIssueNo());
        result.setDrawDate(draw.getDrawDate());
        result.setFavoriteFrontNumbers(favorite.getFrontNumbers());
        result.setFavoriteBackNumbers(favorite.getBackNumbers());
        result.setDrawFrontNumbers(draw.getFrontNumbers());
        result.setDrawBackNumbers(draw.getBackNumbers());
        result.setFrontHitCount(prizeResult.frontHitCount());
        result.setBackHitCount(prizeResult.backHitCount());
        result.setWinning(prizeResult.winning());
        result.setPrizeLevel(prizeResult.prizeLevel());
        result.setPrizeName(prizeResult.prizeName());
        result.setRuleVersion(prizeResult.ruleVersion());
        result.setStakeAmount(stakeAmount(prizeTiers, prizeResult));
        result.setCalculatedTime(LocalDateTime.now());
        return result;
    }

    /**
     * 计算两个号码区的交集数量。
     */
    private int hitCount(List<Integer> selectedNumbers, List<Integer> drawNumbers) {
        Set<Integer> drawNumberSet = drawNumbers.stream().collect(Collectors.toSet());
        return (int) selectedNumbers.stream()
                .filter(drawNumberSet::contains)
                .count();
    }

    /**
     * 解析数据库存储的前后区号码。
     */
    private LotteryDltNumber parseNumber(String frontNumbers, String backNumbers) {
        return numberService.parseSingle(frontNumbers + " + " + backNumbers);
    }

    /**
     * 按奖级名称匹配官方奖级明细中的单注奖金。
     */
    private BigDecimal stakeAmount(List<LotteryPrizeTier> prizeTiers, LotteryDltPrizeResult prizeResult) {
        if (!prizeResult.winning()) {
            return null;
        }

        return prizeTiers.stream()
                .filter(prizeTier -> prizeResult.prizeName().equals(prizeTier.getPrizeName()))
                .map(LotteryPrizeTier::getStakeAmount)
                .findFirst()
                .orElse(null);
    }

    /**
     * 校验开奖数据已经具备生成收藏开奖结果所需的基础字段。
     */
    private void validateDraw(LotteryDraw draw) {
        if (draw == null || draw.getId() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "开奖数据不能为空");
        }
        if (!LotteryType.DLT.code().equals(draw.getLotteryType())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "当前仅支持大乐透收藏开奖结果生成");
        }
        if (draw.getDrawDate() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "开奖日期不能为空");
        }
        if (draw.getFrontNumbers() == null || draw.getBackNumbers() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "开奖号码不能为空");
        }
    }
}
