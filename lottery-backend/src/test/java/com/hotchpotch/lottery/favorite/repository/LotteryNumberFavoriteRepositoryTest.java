package com.hotchpotch.lottery.favorite.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotchpotch.lottery.favorite.entity.LotteryNumberFavorite;
import com.hotchpotch.lottery.favorite.mapper.LotteryNumberFavoriteMapper;
import org.junit.jupiter.api.Test;

class LotteryNumberFavoriteRepositoryTest {

    @Test
    void mapperExtendsMybatisPlusBaseMapper() {
        assertThat(BaseMapper.class).isAssignableFrom(LotteryNumberFavoriteMapper.class);
    }

    @Test
    void repositoryFindsFavoriteById() {
        LotteryNumberFavoriteMapper mapper = mock(LotteryNumberFavoriteMapper.class);
        LotteryNumberFavorite favorite = new LotteryNumberFavorite();
        when(mapper.selectById(10L)).thenReturn(favorite);

        LotteryNumberFavoriteRepository repository = new LotteryNumberFavoriteRepository(mapper);

        assertThat(repository.findById(10L)).containsSame(favorite);
        verify(mapper).selectById(10L);
    }

    @Test
    void repositoryFindsFavoriteByUserAndNumbers() {
        LotteryNumberFavoriteMapper mapper = mock(LotteryNumberFavoriteMapper.class);
        LotteryNumberFavorite favorite = new LotteryNumberFavorite();
        when(mapper.selectOne(anyFavoriteWrapper())).thenReturn(favorite);

        LotteryNumberFavoriteRepository repository = new LotteryNumberFavoriteRepository(mapper);

        assertThat(repository.findByUserAndNumbers(10L, "DLT", "01,05,12,23,35", "03,11"))
                .containsSame(favorite);
        verify(mapper).selectOne(anyFavoriteWrapper());
    }

    @Test
    void repositoryFindsPageAndCountsByUserAndStatus() {
        LotteryNumberFavoriteMapper mapper = mock(LotteryNumberFavoriteMapper.class);
        LotteryNumberFavorite favorite = new LotteryNumberFavorite();
        when(mapper.selectList(anyFavoriteWrapper())).thenReturn(java.util.List.of(favorite));
        when(mapper.selectCount(anyFavoriteWrapper())).thenReturn(1L);

        LotteryNumberFavoriteRepository repository = new LotteryNumberFavoriteRepository(mapper);

        assertThat(repository.findPageByUserIdAndStatus(10L, "ACTIVE", null, 1, 20)).containsExactly(favorite);
        assertThat(repository.countByUserIdAndStatus(10L, "ACTIVE")).isEqualTo(1L);
        verify(mapper).selectList(anyFavoriteWrapper());
        verify(mapper).selectCount(anyFavoriteWrapper());
    }

    @Test
    void repositoryFindsPageAndCountsByKeyword() {
        LotteryNumberFavoriteMapper mapper = mock(LotteryNumberFavoriteMapper.class);
        LotteryNumberFavorite favorite = new LotteryNumberFavorite();
        when(mapper.selectList(anyFavoriteWrapper())).thenReturn(java.util.List.of(favorite));
        when(mapper.selectCount(anyFavoriteWrapper())).thenReturn(1L);

        LotteryNumberFavoriteRepository repository = new LotteryNumberFavoriteRepository(mapper);

        assertThat(repository.findPageByUserIdAndStatus(10L, "ACTIVE", "蓝号", 1, 20)).containsExactly(favorite);
        assertThat(repository.countByUserIdAndStatusAndKeyword(10L, "ACTIVE", "蓝号")).isEqualTo(1L);
        verify(mapper).selectList(anyFavoriteWrapper());
        verify(mapper).selectCount(anyFavoriteWrapper());
    }

    @Test
    void repositoryDelegatesInsertAndUpdateById() {
        LotteryNumberFavoriteMapper mapper = mock(LotteryNumberFavoriteMapper.class);
        LotteryNumberFavorite favorite = new LotteryNumberFavorite();
        when(mapper.insert(favorite)).thenReturn(1);
        when(mapper.updateById(favorite)).thenReturn(1);

        LotteryNumberFavoriteRepository repository = new LotteryNumberFavoriteRepository(mapper);

        assertThat(repository.insert(favorite)).isEqualTo(1);
        assertThat(repository.updateById(favorite)).isEqualTo(1);
        verify(mapper).insert(favorite);
        verify(mapper).updateById(favorite);
    }

    @Test
    void repositoryDeletesFavoriteByUserIdAndId() {
        LotteryNumberFavoriteMapper mapper = mock(LotteryNumberFavoriteMapper.class);
        when(mapper.delete(anyFavoriteWrapper())).thenReturn(1);

        LotteryNumberFavoriteRepository repository = new LotteryNumberFavoriteRepository(mapper);

        assertThat(repository.deleteByUserIdAndId(10L, 20L)).isEqualTo(1);
        verify(mapper).delete(anyFavoriteWrapper());
    }

    private Wrapper<LotteryNumberFavorite> anyFavoriteWrapper() {
        return any();
    }
}
