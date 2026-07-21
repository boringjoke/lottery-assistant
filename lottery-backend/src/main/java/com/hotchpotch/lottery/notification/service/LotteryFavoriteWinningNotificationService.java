package com.hotchpotch.lottery.notification.service;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.draw.entity.LotteryDraw;
import com.hotchpotch.lottery.draw.enums.LotteryType;
import com.hotchpotch.lottery.favorite.entity.LotteryNumberFavorite;
import com.hotchpotch.lottery.favorite.record.LotteryFavoriteDrawHistoryItemResponse;
import com.hotchpotch.lottery.favorite.repository.LotteryNumberFavoriteRepository;
import com.hotchpotch.lottery.favorite.service.LotteryFavoriteAnalyzeService;
import com.hotchpotch.lottery.notification.enums.NotificationBusinessType;
import com.hotchpotch.lottery.notification.enums.NotificationType;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 收藏号码中奖站内通知生成服务。
 */
@Service
public class LotteryFavoriteWinningNotificationService {

    private static final LocalTime DLT_DRAW_TIME = LocalTime.of(20, 30);
    private static final String TITLE = "收藏号码中奖提醒";

    private final LotteryNumberFavoriteRepository favoriteRepository;
    private final LotteryFavoriteAnalyzeService favoriteAnalyzeService;
    private final UserNotificationService notificationService;

    public LotteryFavoriteWinningNotificationService(
            LotteryNumberFavoriteRepository favoriteRepository,
            LotteryFavoriteAnalyzeService favoriteAnalyzeService,
            UserNotificationService notificationService) {
        this.favoriteRepository = favoriteRepository;
        this.favoriteAnalyzeService = favoriteAnalyzeService;
        this.notificationService = notificationService;
    }

    /**
     * 为指定已入库开奖生成收藏号码中奖站内通知。
     */
    @Transactional
    public int generateForDraw(LotteryDraw draw) {
        validateDraw(draw);
        LocalDateTime drawTime = draw.getDrawDate().atTime(DLT_DRAW_TIME);
        List<LotteryNumberFavorite> favorites = favoriteRepository.findActiveAtDrawTime(
                draw.getLotteryType(),
                drawTime);
        int notificationCount = 0;

        for (LotteryNumberFavorite favorite : favorites) {
            LotteryFavoriteDrawHistoryItemResponse result = favoriteAnalyzeService.analyzeDraw(favorite, draw);
            if (!result.winning()) {
                continue;
            }

            notificationService.createNotification(
                    favorite.getUserId(),
                    NotificationType.FAVORITE_WINNING,
                    NotificationBusinessType.LOTTERY_FAVORITE_WINNING,
                    businessKey(draw, favorite),
                    TITLE,
                    content(draw, favorite, result));
            notificationCount++;
        }

        return notificationCount;
    }

    private String businessKey(LotteryDraw draw, LotteryNumberFavorite favorite) {
        return draw.getLotteryType() + ":" + draw.getIssueNo() + ":FAVORITE:" + favorite.getId();
    }

    private String content(
            LotteryDraw draw,
            LotteryNumberFavorite favorite,
            LotteryFavoriteDrawHistoryItemResponse result) {
        return "你收藏的「" + favoriteName(favorite) + "」在大乐透第 "
                + draw.getIssueNo()
                + " 期命中"
                + result.prizeName()
                + "。";
    }

    private String favoriteName(LotteryNumberFavorite favorite) {
        if (favorite.getFavoriteName() != null && !favorite.getFavoriteName().isBlank()) {
            return favorite.getFavoriteName();
        }

        return "大乐透 " + favorite.getFrontNumbers().replace(",", " ")
                + " + "
                + favorite.getBackNumbers().replace(",", " ");
    }

    private void validateDraw(LotteryDraw draw) {
        if (draw == null || draw.getId() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "开奖数据不能为空");
        }
        if (!LotteryType.DLT.code().equals(draw.getLotteryType())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "当前仅支持大乐透收藏中奖通知生成");
        }
        if (draw.getDrawDate() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "开奖日期不能为空");
        }
        if (draw.getIssueNo() == null || draw.getIssueNo().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "开奖期号不能为空");
        }
        if (draw.getFrontNumbers() == null || draw.getBackNumbers() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "开奖号码不能为空");
        }
    }
}
