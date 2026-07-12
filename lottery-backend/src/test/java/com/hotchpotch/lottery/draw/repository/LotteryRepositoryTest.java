package com.hotchpotch.lottery.draw.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.hotchpotch.lottery.draw.entity.LotteryDraw;
import com.hotchpotch.lottery.draw.entity.LotteryPrizeTier;
import com.hotchpotch.lottery.draw.entity.LotterySyncTask;
import com.hotchpotch.lottery.draw.mapper.LotteryDrawMapper;
import com.hotchpotch.lottery.draw.mapper.LotteryPrizeTierMapper;
import com.hotchpotch.lottery.draw.mapper.LotterySyncTaskMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class LotteryRepositoryTest {

    @Test
    void mappersExtendMybatisPlusBaseMapper() {
        assertThat(BaseMapper.class).isAssignableFrom(LotteryDrawMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(LotteryPrizeTierMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(LotterySyncTaskMapper.class);
    }

    @Test
    void drawRepositoryFindsDrawByLotteryTypeAndIssueNo() {
        LotteryDrawMapper mapper = mock(LotteryDrawMapper.class);
        LotteryDraw draw = new LotteryDraw();
        when(mapper.selectOne(anyDrawWrapper())).thenReturn(draw);

        LotteryDrawRepository repository = new LotteryDrawRepository(mapper);

        assertThat(repository.findByLotteryTypeAndIssueNo("DLT", "26076")).containsSame(draw);
        verify(mapper).selectOne(anyDrawWrapper());
    }

    @Test
    void drawRepositoryFindsLatestDrawByLotteryType() {
        LotteryDrawMapper mapper = mock(LotteryDrawMapper.class);
        LotteryDraw draw = new LotteryDraw();
        when(mapper.selectOne(anyDrawWrapper())).thenReturn(draw);

        LotteryDrawRepository repository = new LotteryDrawRepository(mapper);

        assertThat(repository.findLatestByLotteryType("DLT")).containsSame(draw);
        verify(mapper).selectOne(anyDrawWrapper());
    }

    @Test
    void drawRepositoryListsDrawsByLotteryTypeAndCountsTotal() {
        LotteryDrawMapper mapper = mock(LotteryDrawMapper.class);
        LotteryDraw draw = new LotteryDraw();
        when(mapper.selectList(anyDrawWrapper())).thenReturn(List.of(draw));
        when(mapper.selectCount(anyDrawWrapper())).thenReturn(1L);

        LotteryDrawRepository repository = new LotteryDrawRepository(mapper);

        assertThat(repository.findPageByLotteryType("DLT", 1, 20)).containsExactly(draw);
        assertThat(repository.countByLotteryType("DLT")).isEqualTo(1L);
        verify(mapper).selectList(anyDrawWrapper());
        verify(mapper).selectCount(anyDrawWrapper());
    }

    @Test
    void drawRepositoryDelegatesInsertAndUpdateById() {
        LotteryDrawMapper mapper = mock(LotteryDrawMapper.class);
        LotteryDraw draw = new LotteryDraw();
        when(mapper.insert(draw)).thenReturn(1);
        when(mapper.updateById(draw)).thenReturn(1);

        LotteryDrawRepository repository = new LotteryDrawRepository(mapper);

        assertThat(repository.insert(draw)).isEqualTo(1);
        assertThat(repository.updateById(draw)).isEqualTo(1);
    }

    @Test
    void prizeTierRepositoryFindsTiersByDrawIdAndInsertsBatch() {
        LotteryPrizeTierMapper mapper = mock(LotteryPrizeTierMapper.class);
        LotteryPrizeTier tier = new LotteryPrizeTier();
        when(mapper.selectList(anyPrizeTierWrapper())).thenReturn(List.of(tier));
        when(mapper.insert(tier)).thenReturn(1);

        LotteryPrizeTierRepository repository = new LotteryPrizeTierRepository(mapper);

        assertThat(repository.findByDrawId(1L)).containsExactly(tier);
        assertThat(repository.insertBatch(List.of(tier))).isEqualTo(1);
        verify(mapper).selectList(anyPrizeTierWrapper());
        verify(mapper).insert(tier);
    }

    @Test
    void syncTaskRepositoryFindsTaskByTaskNo() {
        LotterySyncTaskMapper mapper = mock(LotterySyncTaskMapper.class);
        LotterySyncTask task = new LotterySyncTask();
        when(mapper.selectOne(anySyncTaskWrapper())).thenReturn(task);

        LotterySyncTaskRepository repository = new LotterySyncTaskRepository(mapper);

        assertThat(repository.findByTaskNo("sync-001")).containsSame(task);
        verify(mapper).selectOne(anySyncTaskWrapper());
    }

    private Wrapper<LotteryDraw> anyDrawWrapper() {
        return any();
    }

    private Wrapper<LotteryPrizeTier> anyPrizeTierWrapper() {
        return any();
    }

    private Wrapper<LotterySyncTask> anySyncTaskWrapper() {
        return any();
    }
}
