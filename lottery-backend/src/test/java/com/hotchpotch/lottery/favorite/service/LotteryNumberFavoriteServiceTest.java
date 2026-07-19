package com.hotchpotch.lottery.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.config.FavoriteProperties;
import com.hotchpotch.lottery.draw.service.LotteryDltNumberService;
import com.hotchpotch.lottery.favorite.entity.LotteryNumberFavorite;
import com.hotchpotch.lottery.favorite.enums.LotteryNumberFavoriteStatus;
import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoriteCreateRequest;
import com.hotchpotch.lottery.favorite.record.LotteryNumberFavoriteUpdateRequest;
import com.hotchpotch.lottery.favorite.repository.LotteryNumberFavoriteRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LotteryNumberFavoriteServiceTest {

    @Mock
    private LotteryNumberFavoriteRepository favoriteRepository;

    /**
     * 验证新增收藏会排序、补零并写入有效收藏。
     */
    @Test
    void createFavoriteInsertsNormalizedActiveFavorite() {
        when(favoriteRepository.findByUserAndNumbers(10L, "DLT", "01,05,12,23,35", "03,11"))
                .thenReturn(Optional.empty());
        when(favoriteRepository.countByUserIdAndStatus(10L, LotteryNumberFavoriteStatus.ACTIVE.code()))
                .thenReturn(0L);
        LotteryNumberFavoriteService service = service(100);

        var response = service.createFavorite(10L, new LotteryNumberFavoriteCreateRequest(
                "dlt",
                List.of(35, 1, 12, 23, 5),
                List.of(11, 3),
                "蓝号观察",
                "本地测试"));

        ArgumentCaptor<LotteryNumberFavorite> captor = ArgumentCaptor.forClass(LotteryNumberFavorite.class);
        verify(favoriteRepository).insert(captor.capture());
        LotteryNumberFavorite inserted = captor.getValue();
        assertThat(inserted.getUserId()).isEqualTo(10L);
        assertThat(inserted.getLotteryType()).isEqualTo("DLT");
        assertThat(inserted.getFrontNumbers()).isEqualTo("01,05,12,23,35");
        assertThat(inserted.getBackNumbers()).isEqualTo("03,11");
        assertThat(inserted.getStatus()).isEqualTo("ACTIVE");
        assertThat(inserted.getFavoriteTime()).isNotNull();
        assertThat(inserted.getEffectiveTime()).isNotNull();
        assertThat(response.displayText()).isEqualTo("01 05 12 23 35 + 03 11");
        assertThat(response.favoriteName()).isEqualTo("蓝号观察");
    }

    /**
     * 验证重复收藏有效记录时复用原记录，不新增第二条。
     */
    @Test
    void createFavoriteReturnsExistingActiveFavorite() {
        LotteryNumberFavorite existing = favorite(
                20L,
                10L,
                "ACTIVE",
                "01,05,12,23,35",
                "03,11");
        when(favoriteRepository.findByUserAndNumbers(10L, "DLT", "01,05,12,23,35", "03,11"))
                .thenReturn(Optional.of(existing));
        LotteryNumberFavoriteService service = service(100);

        var response = service.createFavorite(10L, new LotteryNumberFavoriteCreateRequest(
                "DLT",
                List.of(1, 5, 12, 23, 35),
                List.of(3, 11),
                null,
                null));

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(favoriteRepository, never()).insert(any());
        verify(favoriteRepository, never()).updateById(any());
    }

    /**
     * 验证已取消收藏再次收藏时重新启用同一条记录。
     */
    @Test
    void createFavoriteReactivatesCancelledFavorite() {
        LotteryNumberFavorite existing = favorite(
                20L,
                10L,
                "CANCELLED",
                "01,05,12,23,35",
                "03,11");
        LocalDateTime oldCancelTime = LocalDateTime.of(2026, 7, 18, 12, 0);
        existing.setCancelTime(oldCancelTime);
        when(favoriteRepository.findByUserAndNumbers(10L, "DLT", "01,05,12,23,35", "03,11"))
                .thenReturn(Optional.of(existing));
        when(favoriteRepository.countByUserIdAndStatus(10L, LotteryNumberFavoriteStatus.ACTIVE.code()))
                .thenReturn(0L);
        LotteryNumberFavoriteService service = service(100);

        var response = service.createFavorite(10L, new LotteryNumberFavoriteCreateRequest(
                "DLT",
                List.of(1, 5, 12, 23, 35),
                List.of(3, 11),
                "重新启用",
                null));

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(existing.getCancelTime()).isNull();
        assertThat(existing.getEffectiveTime()).isAfter(oldCancelTime);
        assertThat(existing.getFavoriteName()).isEqualTo("重新启用");
        verify(favoriteRepository).updateById(existing);
        verify(favoriteRepository, never()).insert(any());
    }

    /**
     * 验证达到有效收藏上限后不能继续新增。
     */
    @Test
    void createFavoriteRejectsWhenActiveFavoriteLimitReached() {
        when(favoriteRepository.findByUserAndNumbers(10L, "DLT", "01,05,12,23,35", "03,11"))
                .thenReturn(Optional.empty());
        when(favoriteRepository.countByUserIdAndStatus(10L, LotteryNumberFavoriteStatus.ACTIVE.code()))
                .thenReturn(1L);
        LotteryNumberFavoriteService service = service(1);

        assertThatThrownBy(() -> service.createFavorite(10L, new LotteryNumberFavoriteCreateRequest(
                "DLT",
                List.of(1, 5, 12, 23, 35),
                List.of(3, 11),
                null,
                null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(favoriteRepository, never()).insert(any());
    }

    /**
     * 验证分页查询会规范页码、页大小、状态和关键字。
     */
    @Test
    void listFavoritesNormalizesPageStatusAndKeyword() {
        LotteryNumberFavorite favorite = favorite(20L, 10L, "ACTIVE", "01,05,12,23,35", "03,11");
        when(favoriteRepository.countByUserIdAndStatusAndKeyword(10L, "ACTIVE", "蓝号")).thenReturn(1L);
        when(favoriteRepository.findPageByUserIdAndStatus(10L, "ACTIVE", "蓝号", 1, 100))
                .thenReturn(List.of(favorite));
        LotteryNumberFavoriteService service = service(100);

        var response = service.listFavorites(10L, 0, 200, "active", " 蓝号 ");

        assertThat(response.pageNo()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(100);
        assertThat(response.total()).isEqualTo(1L);
        assertThat(response.pages()).isEqualTo(1);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.keyword()).isEqualTo("蓝号");
        assertThat(response.favorites()).hasSize(1);
    }

    /**
     * 验证只能操作自己的收藏。
     */
    @Test
    void getFavoriteRejectsOtherUserFavoriteAsNotFound() {
        LotteryNumberFavorite favorite = favorite(20L, 99L, "ACTIVE", "01,05,12,23,35", "03,11");
        when(favoriteRepository.findById(20L)).thenReturn(Optional.of(favorite));
        LotteryNumberFavoriteService service = service(100);

        assertThatThrownBy(() -> service.getFavorite(10L, 20L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 验证更新收藏只修改名称和备注。
     */
    @Test
    void updateFavoriteChangesNameAndRemark() {
        LotteryNumberFavorite favorite = favorite(20L, 10L, "ACTIVE", "01,05,12,23,35", "03,11");
        when(favoriteRepository.findById(20L)).thenReturn(Optional.of(favorite));
        LotteryNumberFavoriteService service = service(100);

        var response = service.updateFavorite(10L, 20L, new LotteryNumberFavoriteUpdateRequest(20L, "新名称", "新备注"));

        assertThat(response.favoriteName()).isEqualTo("新名称");
        assertThat(response.remark()).isEqualTo("新备注");
        assertThat(favorite.getFrontNumbers()).isEqualTo("01,05,12,23,35");
        verify(favoriteRepository).updateById(favorite);
    }

    /**
     * 验证取消收藏会标记状态并记录取消时间。
     */
    @Test
    void deactivateFavoriteMarksCancelled() {
        LotteryNumberFavorite favorite = favorite(20L, 10L, "ACTIVE", "01,05,12,23,35", "03,11");
        when(favoriteRepository.findById(20L)).thenReturn(Optional.of(favorite));
        LotteryNumberFavoriteService service = service(100);

        var response = service.deactivateFavorite(10L, 20L);

        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(favorite.getCancelTime()).isNotNull();
        verify(favoriteRepository).updateById(favorite);
    }

    /**
     * 验证已取消收藏可以被当前用户物理删除。
     */
    @Test
    void deleteFavoriteRemovesCancelledFavorite() {
        LotteryNumberFavorite favorite = favorite(20L, 10L, "CANCELLED", "01,05,12,23,35", "03,11");
        when(favoriteRepository.findById(20L)).thenReturn(Optional.of(favorite));
        LotteryNumberFavoriteService service = service(100);

        service.deleteFavorite(10L, 20L);

        verify(favoriteRepository).deleteByUserIdAndId(10L, 20L);
    }

    /**
     * 验证有效收藏必须先取消，不能直接删除。
     */
    @Test
    void deleteFavoriteRejectsActiveFavorite() {
        LotteryNumberFavorite favorite = favorite(20L, 10L, "ACTIVE", "01,05,12,23,35", "03,11");
        when(favoriteRepository.findById(20L)).thenReturn(Optional.of(favorite));
        LotteryNumberFavoriteService service = service(100);

        assertThatThrownBy(() -> service.deleteFavorite(10L, 20L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(favoriteRepository, never()).deleteByUserIdAndId(10L, 20L);
    }

    private LotteryNumberFavoriteService service(int maxActiveCount) {
        FavoriteProperties favoriteProperties = new FavoriteProperties();
        favoriteProperties.setMaxActiveCount(maxActiveCount);

        return new LotteryNumberFavoriteService(
                favoriteRepository,
                new LotteryDltNumberService(),
                favoriteProperties);
    }

    private LotteryNumberFavorite favorite(
            Long id,
            Long userId,
            String status,
            String frontNumbers,
            String backNumbers) {
        LotteryNumberFavorite favorite = new LotteryNumberFavorite();
        favorite.setId(id);
        favorite.setUserId(userId);
        favorite.setLotteryType("DLT");
        favorite.setFrontNumbers(frontNumbers);
        favorite.setBackNumbers(backNumbers);
        favorite.setFavoriteName(null);
        favorite.setRemark(null);
        favorite.setStatus(status);
        favorite.setFavoriteTime(LocalDateTime.of(2026, 7, 18, 10, 0));
        favorite.setEffectiveTime(LocalDateTime.of(2026, 7, 18, 10, 0));
        return favorite;
    }
}
