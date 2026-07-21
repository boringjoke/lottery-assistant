package com.hotchpotch.lottery.notification.service;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.draw.entity.LotteryDraw;
import com.hotchpotch.lottery.draw.enums.LotteryType;
import com.hotchpotch.lottery.favorite.entity.LotteryNumberFavorite;
import com.hotchpotch.lottery.favorite.record.LotteryFavoriteDrawHistoryItemResponse;
import com.hotchpotch.lottery.favorite.repository.LotteryNumberFavoriteRepository;
import com.hotchpotch.lottery.favorite.service.LotteryFavoriteAnalyzeService;
import com.hotchpotch.lottery.mail.record.MailSendRequest;
import com.hotchpotch.lottery.mail.service.MailSendService;
import com.hotchpotch.lottery.notification.enums.NotificationBusinessType;
import com.hotchpotch.lottery.notification.enums.NotificationType;
import com.hotchpotch.lottery.user.entity.LotteryUser;
import com.hotchpotch.lottery.user.entity.LotteryUserCredential;
import com.hotchpotch.lottery.user.enums.UserCredentialType;
import com.hotchpotch.lottery.user.repository.LotteryUserCredentialRepository;
import com.hotchpotch.lottery.user.repository.LotteryUserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 收藏号码中奖通知生成服务。
 */
@Service
public class LotteryFavoriteWinningNotificationService {

    private static final LocalTime DLT_DRAW_TIME = LocalTime.of(20, 30);
    private static final String TITLE = "收藏号码中奖提醒";
    private static final String MAIL_BUSINESS_TYPE = "LOTTERY_DRAW_FAVORITE_WINNING_EMAIL";
    private static final int MAX_MAIL_WINNING_ITEMS = 20;

    private final LotteryNumberFavoriteRepository favoriteRepository;
    private final LotteryFavoriteAnalyzeService favoriteAnalyzeService;
    private final UserNotificationService notificationService;
    private final LotteryUserRepository userRepository;
    private final LotteryUserCredentialRepository credentialRepository;
    private final MailSendService mailSendService;

    public LotteryFavoriteWinningNotificationService(
            LotteryNumberFavoriteRepository favoriteRepository,
            LotteryFavoriteAnalyzeService favoriteAnalyzeService,
            UserNotificationService notificationService) {
        this(
                favoriteRepository,
                favoriteAnalyzeService,
                notificationService,
                null,
                null,
                null);
    }

    @Autowired
    public LotteryFavoriteWinningNotificationService(
            LotteryNumberFavoriteRepository favoriteRepository,
            LotteryFavoriteAnalyzeService favoriteAnalyzeService,
            UserNotificationService notificationService,
            LotteryUserRepository userRepository,
            LotteryUserCredentialRepository credentialRepository,
            MailSendService mailSendService) {
        this.favoriteRepository = favoriteRepository;
        this.favoriteAnalyzeService = favoriteAnalyzeService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.mailSendService = mailSendService;
    }

    /**
     * 为指定已入库开奖生成收藏号码中奖通知。
     *
     * <p>站内通知仍然按“中奖收藏号码”逐条生成；邮件通知按“用户 + 开奖期号”
     * 汇总成一封，避免用户同一期多注中奖时收到多封邮件。</p>
     */
    @Transactional
    public int generateForDraw(LotteryDraw draw) {
        validateDraw(draw);
        LocalDateTime drawTime = draw.getDrawDate().atTime(DLT_DRAW_TIME);
        List<LotteryNumberFavorite> favorites = favoriteRepository.findActiveAtDrawTime(
                draw.getLotteryType(),
                drawTime);
        int notificationCount = 0;
        Map<Long, List<WinningFavorite>> winningFavoritesByUserId = new HashMap<>();

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
            winningFavoritesByUserId
                    .computeIfAbsent(favorite.getUserId(), ignored -> new ArrayList<>())
                    .add(new WinningFavorite(favorite, result));
            notificationCount++;
        }

        sendWinningSummaryEmails(draw, winningFavoritesByUserId);

        return notificationCount;
    }

    /**
     * 按用户汇总发送中奖邮件；单个用户邮件失败不影响其他用户。
     */
    private void sendWinningSummaryEmails(
            LotteryDraw draw,
            Map<Long, List<WinningFavorite>> winningFavoritesByUserId) {
        if (mailSendService == null || userRepository == null || credentialRepository == null) {
            return;
        }

        winningFavoritesByUserId.forEach((userId, winningFavorites) -> {
            try {
                sendWinningSummaryEmail(draw, userId, winningFavorites);
            } catch (RuntimeException ignored) {
                // 邮件发送失败会由 MailSendService 尽量落库；这里兜底避免影响后续用户。
            }
        });
    }

    /**
     * 为单个用户发送一期开奖的中奖汇总邮件。
     */
    private void sendWinningSummaryEmail(
            LotteryDraw draw,
            Long userId,
            List<WinningFavorite> winningFavorites) {
        if (winningFavorites == null || winningFavorites.isEmpty()) {
            return;
        }

        String toEmail = notificationEmail(userId);
        if (toEmail == null) {
            return;
        }

        mailSendService.sendText(new MailSendRequest(
                userId,
                MAIL_BUSINESS_TYPE,
                mailBusinessKey(draw, userId),
                toEmail,
                mailSubject(winningFavorites.size()),
                mailContent(draw, winningFavorites)));
    }

    /**
     * 查询用户通知邮箱；只有开启邮箱通知且绑定 EMAIL 凭证时才返回邮箱。
     */
    private String notificationEmail(Long userId) {
        LotteryUser user = userRepository.findById(userId).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getEmailNotificationEnabled())) {
            return null;
        }

        return credentialRepository
                .findByUserIdAndCredentialType(userId, UserCredentialType.EMAIL.code())
                .map(LotteryUserCredential::getIdentifier)
                .filter(email -> email != null && !email.isBlank())
                .orElse(null);
    }

    private String mailBusinessKey(LotteryDraw draw, Long userId) {
        return draw.getLotteryType() + ":" + draw.getIssueNo() + ":USER:" + userId;
    }

    private String mailSubject(int winningCount) {
        return "彩票助手中奖提醒：你有 " + winningCount + " 注收藏号码中奖";
    }

    private String mailContent(LotteryDraw draw, List<WinningFavorite> winningFavorites) {
        StringBuilder contentBuilder = new StringBuilder()
                .append("大乐透第 ")
                .append(draw.getIssueNo())
                .append(" 期开奖后，你有 ")
                .append(winningFavorites.size())
                .append(" 注收藏号码中奖：\n\n");

        int itemCount = Math.min(winningFavorites.size(), MAX_MAIL_WINNING_ITEMS);
        for (int index = 0; index < itemCount; index++) {
            WinningFavorite winningFavorite = winningFavorites.get(index);
            contentBuilder.append(index + 1)
                    .append(". ")
                    .append(favoriteName(winningFavorite.favorite()))
                    .append("：命中")
                    .append(winningFavorite.result().prizeName())
                    .append("\n");
        }
        if (winningFavorites.size() > MAX_MAIL_WINNING_ITEMS) {
            contentBuilder.append("\n还有 ")
                    .append(winningFavorites.size() - MAX_MAIL_WINNING_ITEMS)
                    .append(" 注中奖收藏号码，请登录彩票助手查看。");
        }

        return contentBuilder.toString();
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

    /**
     * 用户单注收藏号码的中奖结果。
     */
    private record WinningFavorite(
            LotteryNumberFavorite favorite,
            LotteryFavoriteDrawHistoryItemResponse result) {
    }
}
