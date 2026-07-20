package com.hotchpotch.lottery.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.draw.entity.LotteryDraw;
import com.hotchpotch.lottery.draw.repository.LotteryDrawRepository;
import com.hotchpotch.lottery.draw.service.LotteryDltNumberService;
import com.hotchpotch.lottery.draw.service.LotteryDltPrizeRuleService;
import com.hotchpotch.lottery.favorite.entity.LotteryNumberFavorite;
import com.hotchpotch.lottery.favorite.repository.LotteryNumberFavoriteRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LotteryFavoriteAnalyzeServiceTest {

    @Mock
    private LotteryNumberFavoriteRepository favoriteRepository;

    @Mock
    private LotteryDrawRepository drawRepository;

    /**
     * 验证收藏中奖历史实时分析只分页返回中奖记录。
     */
    @Test
    void analyzeFavoriteHistoryReturnsOnlyPagedWinningResults() {
        LotteryNumberFavorite favorite = favorite(20L, 10L);
        LotteryDraw winningDraw = draw("26076", "01,05,12,23,35", "03,11");
        LotteryDraw noPrizeDraw = draw("26075", "02,06,13,24,34", "04,12");
        when(favoriteRepository.findById(20L)).thenReturn(Optional.of(favorite));
        when(drawRepository.findAllByLotteryType("DLT")).thenReturn(List.of(winningDraw, noPrizeDraw));
        when(drawRepository.findLatestByLotteryType("DLT")).thenReturn(Optional.of(winningDraw));
        LotteryFavoriteAnalyzeService service = service();

        var response = service.analyzeFavoriteHistory(10L, 20L, 1, 2);

        assertThat(response.favoriteId()).isEqualTo(20L);
        assertThat(response.total()).isEqualTo(1L);
        assertThat(response.pages()).isEqualTo(1);
        assertThat(response.latestDrawResult().winning()).isTrue();
        assertThat(response.latestDrawResult().prizeName()).isEqualTo("一等奖");
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).prizeLevel()).isEqualTo(1);
        assertThat(response.results())
                .extracting("issueNo")
                .doesNotContain("26075");
    }

    /**
     * 验证收藏列表最近一期结果没有开奖数据时返回空。
     */
    @Test
    void latestDrawResultReturnsNullWhenNoDrawExists() {
        LotteryNumberFavorite favorite = favorite(20L, 10L);
        when(drawRepository.findLatestByLotteryType("DLT")).thenReturn(Optional.empty());
        LotteryFavoriteAnalyzeService service = service();

        assertThat(service.latestDrawResult(favorite)).isNull();
    }

    /**
     * 验证不能查询其他用户的收藏开奖历史。
     */
    @Test
    void analyzeFavoriteHistoryRejectsOtherUserFavoriteAsNotFound() {
        when(favoriteRepository.findById(20L)).thenReturn(Optional.of(favorite(20L, 99L)));
        LotteryFavoriteAnalyzeService service = service();

        assertThatThrownBy(() -> service.analyzeFavoriteHistory(10L, 20L, 1, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    private LotteryFavoriteAnalyzeService service() {
        return new LotteryFavoriteAnalyzeService(
                favoriteRepository,
                drawRepository,
                new LotteryDltNumberService(),
                new LotteryDltPrizeRuleService());
    }

    private LotteryNumberFavorite favorite(Long id, Long userId) {
        LotteryNumberFavorite favorite = new LotteryNumberFavorite();
        favorite.setId(id);
        favorite.setUserId(userId);
        favorite.setLotteryType("DLT");
        favorite.setFrontNumbers("01,05,12,23,35");
        favorite.setBackNumbers("03,11");
        favorite.setFavoriteName("生日组合");
        favorite.setStatus("ACTIVE");
        favorite.setFavoriteTime(LocalDateTime.of(2026, 7, 18, 10, 0));
        favorite.setEffectiveTime(LocalDateTime.of(2026, 7, 18, 10, 0));
        return favorite;
    }

    private LotteryDraw draw(String issueNo, String frontNumbers, String backNumbers) {
        LotteryDraw draw = new LotteryDraw();
        draw.setId(Long.parseLong(issueNo));
        draw.setLotteryType("DLT");
        draw.setIssueNo(issueNo);
        draw.setDrawDate(LocalDate.of(2026, 7, 18));
        draw.setFrontNumbers(frontNumbers);
        draw.setBackNumbers(backNumbers);
        return draw;
    }
}
