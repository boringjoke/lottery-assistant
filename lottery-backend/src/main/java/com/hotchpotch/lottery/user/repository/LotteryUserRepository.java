package com.hotchpotch.lottery.user.repository;

import com.hotchpotch.lottery.user.entity.LotteryUser;
import com.hotchpotch.lottery.user.mapper.LotteryUserMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 彩票助手用户 Repository。
 */
@Repository
public class LotteryUserRepository {

    private final LotteryUserMapper lotteryUserMapper;

    public LotteryUserRepository(LotteryUserMapper lotteryUserMapper) {
        this.lotteryUserMapper = lotteryUserMapper;
    }

    /**
     * 按主键查询用户。
     */
    public Optional<LotteryUser> findById(Long id) {
        LotteryUser user = lotteryUserMapper.selectById(id);

        return Optional.ofNullable(user);
    }

    /**
     * 插入用户。
     */
    public int insert(LotteryUser user) {
        return lotteryUserMapper.insert(user);
    }

    /**
     * 按主键更新用户。
     */
    public int updateById(LotteryUser user) {
        return lotteryUserMapper.updateById(user);
    }
}
