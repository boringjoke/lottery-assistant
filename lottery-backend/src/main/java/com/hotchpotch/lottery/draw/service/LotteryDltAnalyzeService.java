package com.hotchpotch.lottery.draw.service;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.draw.entity.LotteryDraw;
import com.hotchpotch.lottery.draw.enums.LotteryType;
import com.hotchpotch.lottery.draw.record.LotteryDltAnalyzeHitDetail;
import com.hotchpotch.lottery.draw.record.LotteryDltAnalyzeNumberResult;
import com.hotchpotch.lottery.draw.record.LotteryDltAnalyzeRequest;
import com.hotchpotch.lottery.draw.record.LotteryDltAnalyzeResponse;
import com.hotchpotch.lottery.draw.record.LotteryDltNumber;
import com.hotchpotch.lottery.draw.record.LotteryDltPrizeResult;
import com.hotchpotch.lottery.draw.repository.LotteryDrawRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 大乐透号码历史分析服务。
 */
@Service
public class LotteryDltAnalyzeService {

    private static final String NO_PRIZE_NAME = "未中奖";

    private final LotteryDltNumberService numberService;
    private final LotteryDltPrizeRuleService prizeRuleService;
    private final LotteryDrawRepository drawRepository;

    /**
     * 初始化大乐透号码历史分析服务依赖。
     */
    public LotteryDltAnalyzeService(
            LotteryDltNumberService numberService,
            LotteryDltPrizeRuleService prizeRuleService,
            LotteryDrawRepository drawRepository) {
        this.numberService = numberService;
        this.prizeRuleService = prizeRuleService;
        this.drawRepository = drawRepository;
    }

    /**
     * 分析一组大乐透号码在历史开奖中的命中情况。
     */
    public LotteryDltAnalyzeResponse analyze(LotteryDltAnalyzeRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "分析请求不能为空");
        }

        List<LotteryDltNumber> numbers = numberService.parseBatch(request.numbers());
        List<LotteryDraw> draws = drawRepository.findAllByLotteryType(LotteryType.DLT.code());
        List<LotteryDltAnalyzeNumberResult> results = new ArrayList<>();
        for (int index = 0; index < numbers.size(); index++) {
            LotteryDltAnalyzeNumberResult result = analyzeNumber(
                    index + 1,
                    request.numbers().get(index),
                    numbers.get(index),
                    draws);
            results.add(result);
        }

        int winningNumberCount = 0;
        int winningHitCount = 0;
        for (LotteryDltAnalyzeNumberResult result : results) {
            if (result.winning()) {
                winningNumberCount++;
            }
            winningHitCount += result.winningHitCount();
        }
        Integer bestPrizeLevel = bestPrizeLevel(results);

        return new LotteryDltAnalyzeResponse(
                results.size(),
                draws.size(),
                winningNumberCount,
                winningHitCount,
                bestPrizeLevel,
                bestPrizeName(bestPrizeLevel),
                results);
    }

    /**
     * 分析单注号码在历史开奖中的命中情况。
     */
    private LotteryDltAnalyzeNumberResult analyzeNumber(
            int lineNo,
            String inputText,
            LotteryDltNumber number,
            List<LotteryDraw> draws) {
        List<LotteryDltAnalyzeHitDetail> hitDetails = new ArrayList<>();
        for (LotteryDraw draw : draws) {
            LotteryDltAnalyzeHitDetail hitDetail = analyzeDraw(number, draw);
            if (hitDetail.winning()) {
                hitDetails.add(hitDetail);
            }
        }
        Integer bestPrizeLevel = bestPrizeLevelFromHitDetails(hitDetails);

        return new LotteryDltAnalyzeNumberResult(
                lineNo,
                inputText,
                number.displayText(),
                number.frontNumbers(),
                number.backNumbers(),
                !hitDetails.isEmpty(),
                hitDetails.size(),
                bestPrizeLevel,
                bestPrizeName(bestPrizeLevel),
                hitDetails);
    }

    /**
     * 分析单注号码与单期开奖的命中情况。
     */
    private LotteryDltAnalyzeHitDetail analyzeDraw(LotteryDltNumber number, LotteryDraw draw) {
        List<Integer> drawFrontNumbers = parseStoredNumbers(draw.getFrontNumbers(), "开奖前区号码格式不合法");
        List<Integer> drawBackNumbers = parseStoredNumbers(draw.getBackNumbers(), "开奖后区号码格式不合法");
        int frontHitCount = hitCount(number.frontNumbers(), drawFrontNumbers);
        int backHitCount = hitCount(number.backNumbers(), drawBackNumbers);
        LotteryDltPrizeResult prizeResult = prizeRuleService.determinePrize(frontHitCount, backHitCount);

        return new LotteryDltAnalyzeHitDetail(
                draw.getIssueNo(),
                draw.getDrawDate(),
                draw.getFrontNumbers(),
                draw.getBackNumbers(),
                frontHitCount,
                backHitCount,
                prizeResult.winning(),
                prizeResult.prizeLevel(),
                prizeResult.prizeName(),
                prizeResult.ruleVersion());
    }

    /**
     * 解析数据库中逗号分隔的号码文本。
     */
    private List<Integer> parseStoredNumbers(String numbers, String errorMessage) {
        if (numbers == null || numbers.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, errorMessage);
        }

        try {
            List<Integer> parsedNumbers = new ArrayList<>();
            for (String token : Arrays.asList(numbers.split(","))) {
                String trimmedToken = token.trim();
                if (!trimmedToken.isBlank()) {
                    parsedNumbers.add(Integer.parseInt(trimmedToken));
                }
            }
            return parsedNumbers;
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, errorMessage);
        }
    }

    /**
     * 计算一组号码命中另一组号码的数量。
     */
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

    /**
     * 从多注分析结果中取最高奖级序号。
     */
    private Integer bestPrizeLevel(List<LotteryDltAnalyzeNumberResult> results) {
        Integer bestPrizeLevel = null;
        for (LotteryDltAnalyzeNumberResult result : results) {
            if (result.bestPrizeLevel() == null) {
                continue;
            }
            if (bestPrizeLevel == null || result.bestPrizeLevel() < bestPrizeLevel) {
                bestPrizeLevel = result.bestPrizeLevel();
            }
        }

        return bestPrizeLevel;
    }

    /**
     * 从单注命中明细中取最高奖级序号。
     */
    private Integer bestPrizeLevelFromHitDetails(List<LotteryDltAnalyzeHitDetail> hitDetails) {
        Integer bestPrizeLevel = null;
        for (LotteryDltAnalyzeHitDetail hitDetail : hitDetails) {
            if (hitDetail.prizeLevel() == null) {
                continue;
            }
            if (bestPrizeLevel == null || hitDetail.prizeLevel() < bestPrizeLevel) {
                bestPrizeLevel = hitDetail.prizeLevel();
            }
        }

        return bestPrizeLevel;
    }

    /**
     * 按奖级序号返回奖级名称。
     */
    private String bestPrizeName(Integer bestPrizeLevel) {
        if (bestPrizeLevel == null) {
            return NO_PRIZE_NAME;
        }

        return switch (bestPrizeLevel) {
            case 1 -> "一等奖";
            case 2 -> "二等奖";
            case 3 -> "三等奖";
            case 4 -> "四等奖";
            case 5 -> "五等奖";
            case 6 -> "六等奖";
            case 7 -> "七等奖";
            case 8 -> "八等奖";
            case 9 -> "九等奖";
            default -> NO_PRIZE_NAME;
        };
    }
}
