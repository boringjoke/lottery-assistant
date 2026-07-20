package com.hotchpotch.lottery.favorite.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotchpotch.lottery.favorite.entity.LotteryFavoriteDrawResult;
import com.hotchpotch.lottery.favorite.mapper.LotteryFavoriteDrawResultMapper;
import org.junit.jupiter.api.Test;

class LotteryFavoriteDrawResultRepositoryTest {

    @Test
    void mapperExtendsMybatisPlusBaseMapper() {
        assertThat(BaseMapper.class).isAssignableFrom(LotteryFavoriteDrawResultMapper.class);
    }

    @Test
    void repositoryFindsResultById() {
        LotteryFavoriteDrawResultMapper mapper = mock(LotteryFavoriteDrawResultMapper.class);
        LotteryFavoriteDrawResult result = new LotteryFavoriteDrawResult();
        when(mapper.selectById(10L)).thenReturn(result);

        LotteryFavoriteDrawResultRepository repository = new LotteryFavoriteDrawResultRepository(mapper);

        assertThat(repository.findById(10L)).containsSame(result);
        verify(mapper).selectById(10L);
    }

    @Test
    void repositoryFindsResultByFavoriteIdAndDrawId() {
        LotteryFavoriteDrawResultMapper mapper = mock(LotteryFavoriteDrawResultMapper.class);
        LotteryFavoriteDrawResult result = new LotteryFavoriteDrawResult();
        when(mapper.selectOne(anyResultWrapper())).thenReturn(result);

        LotteryFavoriteDrawResultRepository repository = new LotteryFavoriteDrawResultRepository(mapper);

        assertThat(repository.findByFavoriteIdAndDrawId(10L, 20L)).containsSame(result);
        verify(mapper).selectOne(anyResultWrapper());
    }

    @Test
    void repositoryFindsPageAndCountsByUserAndFavorite() {
        LotteryFavoriteDrawResultMapper mapper = mock(LotteryFavoriteDrawResultMapper.class);
        LotteryFavoriteDrawResult result = new LotteryFavoriteDrawResult();
        when(mapper.selectList(anyResultWrapper())).thenReturn(java.util.List.of(result));
        when(mapper.selectCount(anyResultWrapper())).thenReturn(1L);

        LotteryFavoriteDrawResultRepository repository = new LotteryFavoriteDrawResultRepository(mapper);

        assertThat(repository.findPageByUserIdAndFavoriteId(10L, 20L, 1, 20)).containsExactly(result);
        assertThat(repository.countByUserIdAndFavoriteId(10L, 20L)).isEqualTo(1L);
        verify(mapper).selectList(anyResultWrapper());
        verify(mapper).selectCount(anyResultWrapper());
    }

    @Test
    void repositoryFindsPageAndCountsByUserOnly() {
        LotteryFavoriteDrawResultMapper mapper = mock(LotteryFavoriteDrawResultMapper.class);
        LotteryFavoriteDrawResult result = new LotteryFavoriteDrawResult();
        when(mapper.selectList(anyResultWrapper())).thenReturn(java.util.List.of(result));
        when(mapper.selectCount(anyResultWrapper())).thenReturn(1L);

        LotteryFavoriteDrawResultRepository repository = new LotteryFavoriteDrawResultRepository(mapper);

        assertThat(repository.findPageByUserIdAndFavoriteId(10L, null, 1, 20)).containsExactly(result);
        assertThat(repository.countByUserIdAndFavoriteId(10L, null)).isEqualTo(1L);
        verify(mapper).selectList(anyResultWrapper());
        verify(mapper).selectCount(anyResultWrapper());
    }

    @Test
    void repositoryDelegatesInsertAndUpdateById() {
        LotteryFavoriteDrawResultMapper mapper = mock(LotteryFavoriteDrawResultMapper.class);
        LotteryFavoriteDrawResult result = new LotteryFavoriteDrawResult();
        when(mapper.insert(result)).thenReturn(1);
        when(mapper.updateById(result)).thenReturn(1);

        LotteryFavoriteDrawResultRepository repository = new LotteryFavoriteDrawResultRepository(mapper);

        assertThat(repository.insert(result)).isEqualTo(1);
        assertThat(repository.updateById(result)).isEqualTo(1);
        verify(mapper).insert(result);
        verify(mapper).updateById(result);
    }

    private Wrapper<LotteryFavoriteDrawResult> anyResultWrapper() {
        return any();
    }
}
