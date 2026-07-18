package com.hotchpotch.lottery.user.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hotchpotch.lottery.user.entity.LotteryUserOAuthAccount;
import com.hotchpotch.lottery.user.mapper.LotteryUserOAuthAccountMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 彩票助手用户第三方账号 Repository。
 */
@Repository
public class LotteryUserOAuthAccountRepository {

    private final LotteryUserOAuthAccountMapper lotteryUserOAuthAccountMapper;

    public LotteryUserOAuthAccountRepository(LotteryUserOAuthAccountMapper lotteryUserOAuthAccountMapper) {
        this.lotteryUserOAuthAccountMapper = lotteryUserOAuthAccountMapper;
    }

    /**
     * 按第三方平台和 OpenID 查询账号绑定。
     */
    public Optional<LotteryUserOAuthAccount> findByProviderAndOpenId(String provider, String openId) {
        LotteryUserOAuthAccount account = lotteryUserOAuthAccountMapper.selectOne(Wrappers.<LotteryUserOAuthAccount>lambdaQuery()
                .eq(LotteryUserOAuthAccount::getProvider, provider)
                .eq(LotteryUserOAuthAccount::getOpenId, openId));

        return Optional.ofNullable(account);
    }

    /**
     * 查询用户绑定的全部第三方账号。
     */
    public List<LotteryUserOAuthAccount> findByUserId(Long userId) {
        return lotteryUserOAuthAccountMapper.selectList(Wrappers.<LotteryUserOAuthAccount>lambdaQuery()
                .eq(LotteryUserOAuthAccount::getUserId, userId)
                .orderByAsc(LotteryUserOAuthAccount::getProvider)
                .orderByAsc(LotteryUserOAuthAccount::getId));
    }

    /**
     * 按 UnionID 查询微信体系下可能属于同一人的账号绑定。
     */
    public List<LotteryUserOAuthAccount> findByUnionId(String unionId) {
        return lotteryUserOAuthAccountMapper.selectList(Wrappers.<LotteryUserOAuthAccount>lambdaQuery()
                .eq(LotteryUserOAuthAccount::getUnionId, unionId)
                .orderByAsc(LotteryUserOAuthAccount::getProvider)
                .orderByAsc(LotteryUserOAuthAccount::getId));
    }

    /**
     * 插入第三方账号绑定。
     */
    public int insert(LotteryUserOAuthAccount account) {
        return lotteryUserOAuthAccountMapper.insert(account);
    }

    /**
     * 按主键更新第三方账号绑定。
     */
    public int updateById(LotteryUserOAuthAccount account) {
        return lotteryUserOAuthAccountMapper.updateById(account);
    }
}
