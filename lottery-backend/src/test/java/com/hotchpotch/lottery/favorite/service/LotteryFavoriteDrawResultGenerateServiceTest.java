package com.hotchpotch.lottery.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotchpotch.lottery.draw.entity.LotteryDraw;
import com.hotchpotch.lottery.draw.entity.LotteryPrizeTier;
import com.hotchpotch.lottery.draw.repository.LotteryPrizeTierRepository;
import com.hotchpotch.lottery.draw.service.LotteryDltNumberService;
import com.hotchpotch.lottery.draw.service.LotteryDltPrizeRuleService;
import com.hotchpotch.lottery.favorite.entity.LotteryFavoriteDrawResult;
import com.hotchpotch.lottery.favorite.entity.LotteryNumberFavorite;
import com.hotchpotch.lottery.favorite.repository.LotteryFavoriteDrawResultRepository;
import com.hotchpotch.lottery.favorite.repository.LotteryNumberFavoriteRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LotteryFavoriteDrawResultGenerateServiceTest {

    /**
     * 验证有效收藏命中一等奖时会写入收藏开奖结果快照和奖金。
     */
    @Test
    void generateForDrawInsertsWinningFavoriteDrawResult() {
        LotteryNumberFavoriteRepository favoriteRepository = org.mockito.Mockito.mock(LotteryNumberFavoriteRepository.class);
        LotteryFavoriteDrawResultRepository resultRepository = org.mockito.Mockito.mock(LotteryFavoriteDrawResultRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotteryFavoriteDrawResultGenerateService service = newService(
                favoriteRepository,
                resultRepository,
                prizeTierRepository);
        LotteryDraw draw = sampleDraw("01,05,12,23,35", "03,11");
        LotteryNumberFavorite favorite = sampleFavorite("01,05,12,23,35", "03,11");
        LotteryPrizeTier prizeTier = samplePrizeTier("一等奖", "10000000.00");

        when(favoriteRepository.findActiveAtDrawTime("DLT", LocalDateTime.of(2026, 7, 20, 20, 30)))
                .thenReturn(List.of(favorite));
        when(prizeTierRepository.findByDrawId(88L)).thenReturn(List.of(prizeTier));
        when(resultRepository.findByFavoriteIdAndDrawId(10L, 88L)).thenReturn(Optional.empty());

        int generatedCount = service.generateForDraw(draw);

        ArgumentCaptor<LotteryFavoriteDrawResult> resultCaptor =
                ArgumentCaptor.forClass(LotteryFavoriteDrawResult.class);
        verify(resultRepository).insert(resultCaptor.capture());
        LotteryFavoriteDrawResult savedResult = resultCaptor.getValue();
        assertThat(generatedCount).isEqualTo(1);
        assertThat(savedResult.getFavoriteId()).isEqualTo(10L);
        assertThat(savedResult.getUserId()).isEqualTo(20L);
        assertThat(savedResult.getDrawId()).isEqualTo(88L);
        assertThat(savedResult.getLotteryType()).isEqualTo("DLT");
        assertThat(savedResult.getIssueNo()).isEqualTo("26080");
        assertThat(savedResult.getDrawDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(savedResult.getFavoriteFrontNumbers()).isEqualTo("01,05,12,23,35");
        assertThat(savedResult.getFavoriteBackNumbers()).isEqualTo("03,11");
        assertThat(savedResult.getDrawFrontNumbers()).isEqualTo("01,05,12,23,35");
        assertThat(savedResult.getDrawBackNumbers()).isEqualTo("03,11");
        assertThat(savedResult.getFrontHitCount()).isEqualTo(5);
        assertThat(savedResult.getBackHitCount()).isEqualTo(2);
        assertThat(savedResult.getWinning()).isTrue();
        assertThat(savedResult.getPrizeLevel()).isEqualTo(1);
        assertThat(savedResult.getPrizeName()).isEqualTo("一等奖");
        assertThat(savedResult.getRuleVersion()).isEqualTo("DLT_2019");
        assertThat(savedResult.getStakeAmount()).isEqualByComparingTo("10000000.00");
        assertThat(savedResult.getCalculatedTime()).isNotNull();
    }

    /**
     * 验证未中奖收藏也会写入历史结果，便于后续列表筛选。
     */
    @Test
    void generateForDrawInsertsNoPrizeFavoriteDrawResult() {
        LotteryNumberFavoriteRepository favoriteRepository = org.mockito.Mockito.mock(LotteryNumberFavoriteRepository.class);
        LotteryFavoriteDrawResultRepository resultRepository = org.mockito.Mockito.mock(LotteryFavoriteDrawResultRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotteryFavoriteDrawResultGenerateService service = newService(
                favoriteRepository,
                resultRepository,
                prizeTierRepository);
        LotteryDraw draw = sampleDraw("01,05,12,23,35", "03,11");
        LotteryNumberFavorite favorite = sampleFavorite("06,07,08,09,10", "01,02");

        when(favoriteRepository.findActiveAtDrawTime("DLT", LocalDateTime.of(2026, 7, 20, 20, 30)))
                .thenReturn(List.of(favorite));
        when(prizeTierRepository.findByDrawId(88L)).thenReturn(List.of());
        when(resultRepository.findByFavoriteIdAndDrawId(10L, 88L)).thenReturn(Optional.empty());

        int generatedCount = service.generateForDraw(draw);

        ArgumentCaptor<LotteryFavoriteDrawResult> resultCaptor =
                ArgumentCaptor.forClass(LotteryFavoriteDrawResult.class);
        verify(resultRepository).insert(resultCaptor.capture());
        LotteryFavoriteDrawResult savedResult = resultCaptor.getValue();
        assertThat(generatedCount).isEqualTo(1);
        assertThat(savedResult.getFrontHitCount()).isZero();
        assertThat(savedResult.getBackHitCount()).isZero();
        assertThat(savedResult.getWinning()).isFalse();
        assertThat(savedResult.getPrizeLevel()).isNull();
        assertThat(savedResult.getPrizeName()).isEqualTo("未中奖");
        assertThat(savedResult.getStakeAmount()).isNull();
    }

    /**
     * 验证重复生成同一收藏同期开奖时不会重复插入。
     */
    @Test
    void generateForDrawSkipsExistingFavoriteDrawResult() {
        LotteryNumberFavoriteRepository favoriteRepository = org.mockito.Mockito.mock(LotteryNumberFavoriteRepository.class);
        LotteryFavoriteDrawResultRepository resultRepository = org.mockito.Mockito.mock(LotteryFavoriteDrawResultRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotteryFavoriteDrawResultGenerateService service = newService(
                favoriteRepository,
                resultRepository,
                prizeTierRepository);
        LotteryDraw draw = sampleDraw("01,05,12,23,35", "03,11");
        LotteryNumberFavorite favorite = sampleFavorite("01,05,12,23,35", "03,11");

        when(favoriteRepository.findActiveAtDrawTime("DLT", LocalDateTime.of(2026, 7, 20, 20, 30)))
                .thenReturn(List.of(favorite));
        when(prizeTierRepository.findByDrawId(88L)).thenReturn(List.of());
        when(resultRepository.findByFavoriteIdAndDrawId(10L, 88L))
                .thenReturn(Optional.of(new LotteryFavoriteDrawResult()));

        int generatedCount = service.generateForDraw(draw);

        assertThat(generatedCount).isZero();
        verify(resultRepository, never()).insert(any());
    }

    /**
     * 验证没有有效收藏时不写入任何收藏开奖结果。
     */
    @Test
    void generateForDrawDoesNotInsertWhenNoActiveFavoritesAtDrawTime() {
        LotteryNumberFavoriteRepository favoriteRepository = org.mockito.Mockito.mock(LotteryNumberFavoriteRepository.class);
        LotteryFavoriteDrawResultRepository resultRepository = org.mockito.Mockito.mock(LotteryFavoriteDrawResultRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotteryFavoriteDrawResultGenerateService service = newService(
                favoriteRepository,
                resultRepository,
                prizeTierRepository);
        LotteryDraw draw = sampleDraw("01,05,12,23,35", "03,11");

        when(favoriteRepository.findActiveAtDrawTime("DLT", LocalDateTime.of(2026, 7, 20, 20, 30)))
                .thenReturn(List.of());
        when(prizeTierRepository.findByDrawId(88L)).thenReturn(List.of());

        int generatedCount = service.generateForDraw(draw);

        assertThat(generatedCount).isZero();
        verify(resultRepository, never()).insert(any());
    }

    private LotteryFavoriteDrawResultGenerateService newService(
            LotteryNumberFavoriteRepository favoriteRepository,
            LotteryFavoriteDrawResultRepository resultRepository,
            LotteryPrizeTierRepository prizeTierRepository) {
        return new LotteryFavoriteDrawResultGenerateService(
                favoriteRepository,
                resultRepository,
                prizeTierRepository,
                new LotteryDltNumberService(),
                new LotteryDltPrizeRuleService());
    }

    private LotteryDraw sampleDraw(String frontNumbers, String backNumbers) {
        LotteryDraw draw = new LotteryDraw();
        draw.setId(88L);
        draw.setLotteryType("DLT");
        draw.setIssueNo("26080");
        draw.setDrawDate(LocalDate.of(2026, 7, 20));
        draw.setFrontNumbers(frontNumbers);
        draw.setBackNumbers(backNumbers);
        return draw;
    }

    private LotteryNumberFavorite sampleFavorite(String frontNumbers, String backNumbers) {
        LotteryNumberFavorite favorite = new LotteryNumberFavorite();
        favorite.setId(10L);
        favorite.setUserId(20L);
        favorite.setLotteryType("DLT");
        favorite.setFrontNumbers(frontNumbers);
        favorite.setBackNumbers(backNumbers);
        return favorite;
    }

    private LotteryPrizeTier samplePrizeTier(String prizeName, String stakeAmount) {
        LotteryPrizeTier prizeTier = new LotteryPrizeTier();
        prizeTier.setPrizeName(prizeName);
        prizeTier.setStakeAmount(new BigDecimal(stakeAmount));
        return prizeTier;
    }
}
