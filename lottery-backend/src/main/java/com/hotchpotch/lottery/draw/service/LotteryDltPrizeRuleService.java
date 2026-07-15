package com.hotchpotch.lottery.draw.service;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.draw.record.LotteryDltPrizeResult;
import org.springframework.stereotype.Service;

/**
 * 大乐透奖级规则判定服务。
 */
@Service
public class LotteryDltPrizeRuleService {

    private static final String RULE_VERSION = "DLT_2019";
    private static final String NO_PRIZE_NAME = "未中奖";

    /**
     * 根据前区和后区命中数量判定大乐透奖级。
     */
    public LotteryDltPrizeResult determinePrize(int frontHitCount, int backHitCount) {
        validateHitCount(frontHitCount, backHitCount);

        if (frontHitCount == 5 && backHitCount == 2) {
            return prize(frontHitCount, backHitCount, 1, "一等奖");
        }
        if (frontHitCount == 5 && backHitCount == 1) {
            return prize(frontHitCount, backHitCount, 2, "二等奖");
        }
        if (frontHitCount == 5 && backHitCount == 0) {
            return prize(frontHitCount, backHitCount, 3, "三等奖");
        }
        if (frontHitCount == 4 && backHitCount == 2) {
            return prize(frontHitCount, backHitCount, 4, "四等奖");
        }
        if (frontHitCount == 4 && backHitCount == 1) {
            return prize(frontHitCount, backHitCount, 5, "五等奖");
        }
        if (frontHitCount == 3 && backHitCount == 2) {
            return prize(frontHitCount, backHitCount, 6, "六等奖");
        }
        if (frontHitCount == 4 && backHitCount == 0) {
            return prize(frontHitCount, backHitCount, 7, "七等奖");
        }
        if ((frontHitCount == 3 && backHitCount == 1)
                || (frontHitCount == 2 && backHitCount == 2)) {
            return prize(frontHitCount, backHitCount, 8, "八等奖");
        }
        if ((frontHitCount == 3 && backHitCount == 0)
                || (frontHitCount == 2 && backHitCount == 1)
                || (frontHitCount == 1 && backHitCount == 2)
                || (frontHitCount == 0 && backHitCount == 2)) {
            return prize(frontHitCount, backHitCount, 9, "九等奖");
        }

        return new LotteryDltPrizeResult(
                frontHitCount,
                backHitCount,
                false,
                null,
                NO_PRIZE_NAME,
                RULE_VERSION);
    }

    /**
     * 校验命中数量是否在大乐透号码范围内。
     */
    private void validateHitCount(int frontHitCount, int backHitCount) {
        if (frontHitCount < 0 || frontHitCount > 5) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "前区命中数必须在 0-5 之间");
        }
        if (backHitCount < 0 || backHitCount > 2) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "后区命中数必须在 0-2 之间");
        }
    }

    /**
     * 创建中奖奖级判定结果。
     */
    private LotteryDltPrizeResult prize(
            int frontHitCount,
            int backHitCount,
            int prizeLevel,
            String prizeName) {
        return new LotteryDltPrizeResult(
                frontHitCount,
                backHitCount,
                true,
                prizeLevel,
                prizeName,
                RULE_VERSION);
    }
}
