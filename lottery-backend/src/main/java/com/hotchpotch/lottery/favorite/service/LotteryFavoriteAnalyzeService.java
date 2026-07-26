package com.hotchpotch.lottery.favorite.service;

import com.hotchpotch.lottery.common.constant.PageConstants;
import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.draw.entity.LotteryDraw;
import com.hotchpotch.lottery.draw.enums.LotteryType;
import com.hotchpotch.lottery.draw.record.LotteryDltNumber;
import com.hotchpotch.lottery.draw.record.LotteryDltPrizeResult;
import com.hotchpotch.lottery.draw.repository.LotteryDrawRepository;
import com.hotchpotch.lottery.draw.service.LotteryDltNumberService;
import com.hotchpotch.lottery.draw.service.LotteryDltPrizeRuleService;
import com.hotchpotch.lottery.favorite.entity.LotteryNumberFavorite;
import com.hotchpotch.lottery.favorite.record.LotteryFavoriteDrawHistoryItemResponse;
import com.hotchpotch.lottery.favorite.record.LotteryFavoriteDrawHistoryPageResponse;
import com.hotchpotch.lottery.favorite.record.LotteryFavoritePrizeCountResponse;
import com.hotchpotch.lottery.favorite.record.LotteryFavoriteWinningSummaryResponse;
import com.hotchpotch.lottery.favorite.repository.LotteryNumberFavoriteRepository;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 收藏号码中奖历史实时分析服务。
 */
@Service
public class LotteryFavoriteAnalyzeService {

    private final LotteryNumberFavoriteRepository favoriteRepository;
    private final LotteryDrawRepository drawRepository;
    private final LotteryDltNumberService numberService;
    private final LotteryDltPrizeRuleService prizeRuleService;

    public LotteryFavoriteAnalyzeService(
            LotteryNumberFavoriteRepository favoriteRepository,
            LotteryDrawRepository drawRepository,
            LotteryDltNumberService numberService,
            LotteryDltPrizeRuleService prizeRuleService) {
        this.favoriteRepository = favoriteRepository;
        this.drawRepository = drawRepository;
        this.numberService = numberService;
        this.prizeRuleService = prizeRuleService;
    }

    /**
     * 分页分析当前用户指定收藏号码的历史中奖表现。
     */
    public LotteryFavoriteDrawHistoryPageResponse analyzeFavoriteHistory(
            Long userId,
            Long favoriteId,
            int pageNo,
            int pageSize) {
        LotteryNumberFavorite favorite = findOwnedFavorite(userId, favoriteId);
        requireDltFavorite(favorite);
        int safePageNo = Math.max(pageNo, PageConstants.DEFAULT_PAGE_NO);
        int safePageSize = Math.min(Math.max(pageSize, 1), PageConstants.MAX_PAGE_SIZE);
        LotteryDltNumber favoriteNumber = parseFavoriteNumber(favorite);
        List<LotteryFavoriteDrawHistoryItemResponse> winningResults = drawRepository
                .findAllByLotteryType(favorite.getLotteryType())
                .stream()
                .map(draw -> analyzeDraw(favoriteNumber, draw))
                .filter(LotteryFavoriteDrawHistoryItemResponse::winning)
                .toList();
        long total = winningResults.size();
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / safePageSize);
        List<LotteryFavoriteDrawHistoryItemResponse> results = pageItems(winningResults, safePageNo, safePageSize);

        return new LotteryFavoriteDrawHistoryPageResponse(
                safePageNo,
                safePageSize,
                total,
                pages,
                favorite.getId(),
                favorite.getLotteryType(),
                favorite.getFrontNumbers(),
                favorite.getBackNumbers(),
                displayText(favorite),
                latestDrawResult(favorite),
                summarizeWinningResults(winningResults),
                results);
    }

    /**
     * 统计收藏号码的历史中奖摘要。
     */
    public LotteryFavoriteWinningSummaryResponse winningSummary(LotteryNumberFavorite favorite) {
        if (favorite == null || favorite.getLotteryType() == null) {
            return emptyWinningSummary();
        }
        if (!LotteryType.DLT.code().equals(favorite.getLotteryType())) {
            return emptyWinningSummary();
        }

        LotteryDltNumber favoriteNumber = parseFavoriteNumber(favorite);
        List<LotteryFavoriteDrawHistoryItemResponse> winningResults = drawRepository
                .findAllByLotteryType(favorite.getLotteryType())
                .stream()
                .map(draw -> analyzeDraw(favoriteNumber, draw))
                .filter(LotteryFavoriteDrawHistoryItemResponse::winning)
                .toList();

        return summarizeWinningResults(winningResults);
    }

    /**
     * 分析收藏号码在最近一期开奖中的表现。
     */
    public LotteryFavoriteDrawHistoryItemResponse latestDrawResult(LotteryNumberFavorite favorite) {
        if (favorite == null || favorite.getLotteryType() == null) {
            return null;
        }
        if (!LotteryType.DLT.code().equals(favorite.getLotteryType())) {
            return null;
        }

        LotteryDltNumber favoriteNumber = parseFavoriteNumber(favorite);

        return drawRepository.findLatestByLotteryType(favorite.getLotteryType())
                .map(draw -> analyzeDraw(favoriteNumber, draw))
                .orElse(null);
    }

    /**
     * 分析单注收藏号码与单期开奖的命中结果。
     */
    public LotteryFavoriteDrawHistoryItemResponse analyzeDraw(LotteryNumberFavorite favorite, LotteryDraw draw) {
        requireDltFavorite(favorite);

        return analyzeDraw(parseFavoriteNumber(favorite), draw);
    }

    /**
     * 分析单注号码与单期开奖的命中结果。
     */
    public LotteryFavoriteDrawHistoryItemResponse analyzeDraw(LotteryDltNumber favoriteNumber, LotteryDraw draw) {
        LotteryDltNumber drawNumber = numberService.parseSingle(draw.getFrontNumbers() + " + " + draw.getBackNumbers());
        int frontHitCount = hitCount(favoriteNumber.frontNumbers(), drawNumber.frontNumbers());
        int backHitCount = hitCount(favoriteNumber.backNumbers(), drawNumber.backNumbers());
        LotteryDltPrizeResult prizeResult = prizeRuleService.determinePrize(frontHitCount, backHitCount);

        return new LotteryFavoriteDrawHistoryItemResponse(
                draw.getIssueNo(),
                draw.getDrawDate(),
                draw.getFrontNumbers(),
                draw.getBackNumbers(),
                prizeResult.frontHitCount(),
                prizeResult.backHitCount(),
                prizeResult.winning(),
                prizeResult.prizeLevel(),
                prizeResult.prizeName(),
                prizeResult.ruleVersion());
    }

    private LotteryNumberFavorite findOwnedFavorite(Long userId, Long favoriteId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (favoriteId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "收藏 ID 不能为空");
        }

        LotteryNumberFavorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(this::favoriteNotFound);
        if (!userId.equals(favorite.getUserId())) {
            throw favoriteNotFound();
        }

        return favorite;
    }

    private void requireDltFavorite(LotteryNumberFavorite favorite) {
        if (!LotteryType.DLT.code().equals(favorite.getLotteryType())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "当前仅支持大乐透收藏开奖历史分析");
        }
    }

    private LotteryDltNumber parseFavoriteNumber(LotteryNumberFavorite favorite) {
        return numberService.parseSingle(displayText(favorite));
    }

    private String displayText(LotteryNumberFavorite favorite) {
        return favorite.getFrontNumbers().replace(",", " ")
                + " + "
                + favorite.getBackNumbers().replace(",", " ");
    }

    private int hitCount(List<Integer> numbers, List<Integer> drawNumbers) {
        Set<Integer> drawNumberSet = new HashSet<>(drawNumbers);
        int hitCount = 0;
        for (Integer number : numbers) {
            if (drawNumberSet.contains(number)) {
                hitCount++;
            }
        }

        return hitCount;
    }

    private List<LotteryFavoriteDrawHistoryItemResponse> pageItems(
            List<LotteryFavoriteDrawHistoryItemResponse> results,
            int pageNo,
            int pageSize) {
        int fromIndex = Math.min((pageNo - 1) * pageSize, results.size());
        int toIndex = Math.min(fromIndex + pageSize, results.size());

        return results.subList(fromIndex, toIndex);
    }

    /**
     * 根据完整中奖结果统计历史最高奖项和各奖级命中次数。
     */
    private LotteryFavoriteWinningSummaryResponse summarizeWinningResults(
            List<LotteryFavoriteDrawHistoryItemResponse> winningResults) {
        if (winningResults == null || winningResults.isEmpty()) {
            return emptyWinningSummary();
        }

        LotteryFavoriteDrawHistoryItemResponse bestResult = null;
        Map<Integer, PrizeCountAccumulator> prizeCountMap = new LinkedHashMap<>();
        for (LotteryFavoriteDrawHistoryItemResponse result : winningResults) {
            if (!result.winning() || result.prizeLevel() == null) {
                continue;
            }

            prizeCountMap.computeIfAbsent(
                    result.prizeLevel(),
                    prizeLevel -> new PrizeCountAccumulator(prizeLevel, result.prizeName()))
                    .increase();
            if (isBetterWinningResult(result, bestResult)) {
                bestResult = result;
            }
        }

        if (bestResult == null) {
            return emptyWinningSummary();
        }

        List<LotteryFavoritePrizeCountResponse> prizeCounts = prizeCountMap.values()
                .stream()
                .sorted((left, right) -> Integer.compare(left.prizeLevel(), right.prizeLevel()))
                .map(PrizeCountAccumulator::toResponse)
                .toList();

        return new LotteryFavoriteWinningSummaryResponse(
                true,
                winningResults.size(),
                bestResult.prizeLevel(),
                bestResult.prizeName(),
                bestResult.issueNo(),
                bestResult.drawDate(),
                prizeCounts);
    }

    /**
     * 判断候选结果是否优于当前历史最佳；同奖级时保留最新一期。
     */
    private boolean isBetterWinningResult(
            LotteryFavoriteDrawHistoryItemResponse candidate,
            LotteryFavoriteDrawHistoryItemResponse currentBest) {
        if (candidate == null || candidate.prizeLevel() == null) {
            return false;
        }
        if (currentBest == null || currentBest.prizeLevel() == null) {
            return true;
        }
        if (!candidate.prizeLevel().equals(currentBest.prizeLevel())) {
            return candidate.prizeLevel() < currentBest.prizeLevel();
        }
        if (candidate.drawDate() != null && currentBest.drawDate() != null
                && !candidate.drawDate().equals(currentBest.drawDate())) {
            return candidate.drawDate().isAfter(currentBest.drawDate());
        }
        if (candidate.issueNo() == null || currentBest.issueNo() == null) {
            return false;
        }

        return candidate.issueNo().compareTo(currentBest.issueNo()) > 0;
    }

    /**
     * 构造无中奖历史的空摘要。
     */
    private LotteryFavoriteWinningSummaryResponse emptyWinningSummary() {
        return new LotteryFavoriteWinningSummaryResponse(false, 0, null, null, null, null, List.of());
    }

    private BusinessException favoriteNotFound() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "收藏号码不存在");
    }

    /**
     * 奖级命中次数累加器。
     */
    private static final class PrizeCountAccumulator {

        private final Integer prizeLevel;
        private final String prizeName;
        private long count;

        /**
         * 初始化奖级统计项。
         */
        private PrizeCountAccumulator(Integer prizeLevel, String prizeName) {
            this.prizeLevel = prizeLevel;
            this.prizeName = prizeName;
        }

        private Integer prizeLevel() {
            return prizeLevel;
        }

        private void increase() {
            count++;
        }

        private LotteryFavoritePrizeCountResponse toResponse() {
            return new LotteryFavoritePrizeCountResponse(prizeLevel, prizeName, count);
        }
    }
}
