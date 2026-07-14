package com.hotchpotch.lottery.draw.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hotchpotch.lottery.draw.entity.LotterySyncTask;
import com.hotchpotch.lottery.draw.mapper.LotterySyncTaskMapper;
import java.util.List;
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
     * 按状态分页查询同步任务；状态为空时查询全部任务，按创建时间倒序返回。
     */
    public List<LotterySyncTask> findPageByStatus(String status, int pageNo, int pageSize) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePageNo - 1) * safePageSize;
        LambdaQueryWrapper<LotterySyncTask> query = statusQuery(status)
                .orderByDesc(LotterySyncTask::getCreateTime)
                .orderByDesc(LotterySyncTask::getId)
                .last("LIMIT " + safePageSize + " OFFSET " + offset);

        return lotterySyncTaskMapper.selectList(query);
    }

    /**
     * 按状态统计同步任务数量；状态为空时统计全部任务。
     */
    public Long countByStatus(String status) {
        return lotterySyncTaskMapper.selectCount(statusQuery(status));
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

    /**
     * 创建可选状态筛选条件。
     */
    private LambdaQueryWrapper<LotterySyncTask> statusQuery(String status) {
        LambdaQueryWrapper<LotterySyncTask> query = Wrappers.lambdaQuery();
        if (status != null && !status.isBlank()) {
            query.eq(LotterySyncTask::getStatus, status);
        }

        return query;
    }
}
