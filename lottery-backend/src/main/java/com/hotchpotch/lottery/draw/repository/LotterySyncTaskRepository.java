package com.hotchpotch.lottery.draw.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hotchpotch.lottery.draw.entity.LotterySyncTask;
import com.hotchpotch.lottery.draw.mapper.LotterySyncTaskMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 彩票开奖同步任务 Repository。
 */
@Repository
public class LotterySyncTaskRepository {

    private final LotterySyncTaskMapper lotterySyncTaskMapper;

    public LotterySyncTaskRepository(LotterySyncTaskMapper lotterySyncTaskMapper) {
        this.lotterySyncTaskMapper = lotterySyncTaskMapper;
    }

    /**
     * 按任务编号查询同步任务。
     */
    public Optional<LotterySyncTask> findByTaskNo(String taskNo) {
        LotterySyncTask task = lotterySyncTaskMapper.selectOne(Wrappers.<LotterySyncTask>lambdaQuery()
                .eq(LotterySyncTask::getTaskNo, taskNo));

        return Optional.ofNullable(task);
    }

    /**
     * 插入同步任务。
     */
    public int insert(LotterySyncTask task) {
        return lotterySyncTaskMapper.insert(task);
    }

    /**
     * 按主键更新同步任务。
     */
    public int updateById(LotterySyncTask task) {
        return lotterySyncTaskMapper.updateById(task);
    }
}
