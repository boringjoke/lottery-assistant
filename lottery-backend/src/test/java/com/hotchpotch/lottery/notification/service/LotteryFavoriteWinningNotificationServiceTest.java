package com.hotchpotch.lottery.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.draw.entity.LotteryDraw;
import com.hotchpotch.lottery.favorite.entity.LotteryNumberFavorite;
import com.hotchpotch.lottery.favorite.record.LotteryFavoriteDrawHistoryItemResponse;
import com.hotchpotch.lottery.favorite.repository.LotteryNumberFavoriteRepository;
import com.hotchpotch.lottery.favorite.service.LotteryFavoriteAnalyzeService;
import com.hotchpotch.lottery.notification.enums.NotificationBusinessType;
import com.hotchpotch.lottery.notification.enums.NotificationType;
import com.hotchpotch.lottery.notification.record.UserNotificationResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LotteryFavoriteWinningNotificationServiceTest {

    @Mock
    private LotteryNumberFavoriteRepository favoriteRepository;

    @Mock
    private LotteryFavoriteAnalyzeService favoriteAnalyzeService;

    @Mock
    private UserNotificationService notificationService;

    /**
     * 验证开奖对应收藏中奖时会创建站内通知。
     */
    @Test
    void generateForDrawCreatesNotificationsForWinningFavorites() {
        LotteryDraw draw = draw();
        LotteryNumberFavorite winningFavorite = favorite(101L, 10L, "生日组合");
        LotteryNumberFavorite noPrizeFavorite = favorite(102L, 10L, "备用号码");
        when(favoriteRepository.findActiveAtDrawTime("DLT", LocalDateTime.of(2026, 7, 18, 20, 30)))
                .thenReturn(List.of(winningFavorite, noPrizeFavorite));
        when(favoriteAnalyzeService.analyzeDraw(winningFavorite, draw)).thenReturn(result(true, "一等奖", 1));
        when(favoriteAnalyzeService.analyzeDraw(noPrizeFavorite, draw)).thenReturn(result(false, "未中奖", null));
        when(notificationService.createNotification(any(), any(), any(), any(), any(), any()))
                .thenReturn(notificationResponse());
        LotteryFavoriteWinningNotificationService service = service();

        int notificationCount = service.generateForDraw(draw);

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<NotificationType> notificationTypeCaptor = ArgumentCaptor.forClass(NotificationType.class);
        ArgumentCaptor<NotificationBusinessType> businessTypeCaptor =
                ArgumentCaptor.forClass(NotificationBusinessType.class);
        ArgumentCaptor<String> businessKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(
                userIdCaptor.capture(),
                notificationTypeCaptor.capture(),
                businessTypeCaptor.capture(),
                businessKeyCaptor.capture(),
                titleCaptor.capture(),
                contentCaptor.capture());
        assertThat(notificationCount).isEqualTo(1);
        assertThat(userIdCaptor.getValue()).isEqualTo(10L);
        assertThat(notificationTypeCaptor.getValue()).isEqualTo(NotificationType.FAVORITE_WINNING);
        assertThat(businessTypeCaptor.getValue()).isEqualTo(NotificationBusinessType.LOTTERY_FAVORITE_WINNING);
        assertThat(businessKeyCaptor.getValue()).isEqualTo("DLT:26076:FAVORITE:101");
        assertThat(titleCaptor.getValue()).isEqualTo("收藏号码中奖提醒");
        assertThat(contentCaptor.getValue()).isEqualTo("你收藏的「生日组合」在大乐透第 26076 期命中一等奖。");
    }

    /**
     * 验证没有有效收藏时不会创建通知。
     */
    @Test
    void generateForDrawDoesNotCreateNotificationWhenNoActiveFavorites() {
        LotteryDraw draw = draw();
        when(favoriteRepository.findActiveAtDrawTime("DLT", LocalDateTime.of(2026, 7, 18, 20, 30)))
                .thenReturn(List.of());
        LotteryFavoriteWinningNotificationService service = service();

        int notificationCount = service.generateForDraw(draw);

        assertThat(notificationCount).isZero();
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
    }

    /**
     * 验证收藏未命名时使用号码作为通知内容里的收藏名。
     */
    @Test
    void generateForDrawUsesNumberDisplayNameWhenFavoriteNameIsBlank() {
        LotteryDraw draw = draw();
        LotteryNumberFavorite favorite = favorite(101L, 10L, " ");
        when(favoriteRepository.findActiveAtDrawTime("DLT", LocalDateTime.of(2026, 7, 18, 20, 30)))
                .thenReturn(List.of(favorite));
        when(favoriteAnalyzeService.analyzeDraw(favorite, draw)).thenReturn(result(true, "九等奖", 9));
        when(notificationService.createNotification(any(), any(), any(), any(), any(), any()))
                .thenReturn(notificationResponse());
        LotteryFavoriteWinningNotificationService service = service();

        service.generateForDraw(draw);

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(any(), any(), any(), any(), any(), contentCaptor.capture());
        assertThat(contentCaptor.getValue())
                .isEqualTo("你收藏的「大乐透 01 02 03 04 05 + 06 07」在大乐透第 26076 期命中九等奖。");
    }

    /**
     * 验证开奖数据缺少必要字段时拒绝生成。
     */
    @Test
    void generateForDrawRejectsInvalidDraw() {
        LotteryFavoriteWinningNotificationService service = service();

        assertThatThrownBy(() -> service.generateForDraw(new LotteryDraw()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    private LotteryFavoriteWinningNotificationService service() {
        return new LotteryFavoriteWinningNotificationService(
                favoriteRepository,
                favoriteAnalyzeService,
                notificationService);
    }

    private LotteryDraw draw() {
        LotteryDraw draw = new LotteryDraw();
        draw.setId(88L);
        draw.setLotteryType("DLT");
        draw.setIssueNo("26076");
        draw.setDrawDate(LocalDate.of(2026, 7, 18));
        draw.setFrontNumbers("01,02,03,04,05");
        draw.setBackNumbers("06,07");
        return draw;
    }

    private LotteryNumberFavorite favorite(Long id, Long userId, String favoriteName) {
        LotteryNumberFavorite favorite = new LotteryNumberFavorite();
        favorite.setId(id);
        favorite.setUserId(userId);
        favorite.setLotteryType("DLT");
        favorite.setFrontNumbers("01,02,03,04,05");
        favorite.setBackNumbers("06,07");
        favorite.setFavoriteName(favoriteName);
        return favorite;
    }

    private LotteryFavoriteDrawHistoryItemResponse result(boolean winning, String prizeName, Integer prizeLevel) {
        return new LotteryFavoriteDrawHistoryItemResponse(
                "26076",
                LocalDate.of(2026, 7, 18),
                "01,02,03,04,05",
                "06,07",
                winning ? 5 : 0,
                winning ? 2 : 0,
                winning,
                prizeLevel,
                prizeName,
                "DLT_2019");
    }

    private UserNotificationResponse notificationResponse() {
        return new UserNotificationResponse(
                1L,
                10L,
                "FAVORITE_WINNING",
                "LOTTERY_FAVORITE_WINNING",
                "DLT:26076:FAVORITE:101",
                "收藏号码中奖提醒",
                "你收藏的号码中奖啦",
                "UNREAD",
                null,
                LocalDateTime.of(2026, 7, 21, 9, 0),
                LocalDateTime.of(2026, 7, 21, 9, 0));
    }
}
