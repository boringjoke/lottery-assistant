package com.hotchpotch.lottery.user.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hotchpotch.lottery.user.entity.LotteryUserRole;
import com.hotchpotch.lottery.user.mapper.LotteryUserRoleMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 彩票助手用户角色 Repository。
 */
@Repository
public class LotteryUserRoleRepository {

    private final LotteryUserRoleMapper lotteryUserRoleMapper;

    public LotteryUserRoleRepository(LotteryUserRoleMapper lotteryUserRoleMapper) {
        this.lotteryUserRoleMapper = lotteryUserRoleMapper;
    }

    /**
     * 查询用户拥有的角色。
     */
    public List<LotteryUserRole> findByUserId(Long userId) {
        return lotteryUserRoleMapper.selectList(Wrappers.<LotteryUserRole>lambdaQuery()
                .eq(LotteryUserRole::getUserId, userId)
                .orderByAsc(LotteryUserRole::getRoleCode)
                .orderByAsc(LotteryUserRole::getId));
    }

    /**
     * 批量插入用户角色。
     */
    public int insertBatch(List<LotteryUserRole> roles) {
        int affectedRows = 0;
        for (LotteryUserRole role : roles) {
            affectedRows += lotteryUserRoleMapper.insert(role);
        }

        return affectedRows;
    }

    /**
     * 删除用户的全部角色。
     */
    public int deleteByUserId(Long userId) {
        return lotteryUserRoleMapper.delete(Wrappers.<LotteryUserRole>lambdaQuery()
                .eq(LotteryUserRole::getUserId, userId));
    }
}
