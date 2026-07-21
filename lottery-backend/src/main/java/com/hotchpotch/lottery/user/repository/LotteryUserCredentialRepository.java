package com.hotchpotch.lottery.user.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hotchpotch.lottery.user.entity.LotteryUserCredential;
import com.hotchpotch.lottery.user.mapper.LotteryUserCredentialMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 彩票助手用户登录凭证 Repository。
 */
@Repository
public class LotteryUserCredentialRepository {

    private final LotteryUserCredentialMapper lotteryUserCredentialMapper;

    public LotteryUserCredentialRepository(LotteryUserCredentialMapper lotteryUserCredentialMapper) {
        this.lotteryUserCredentialMapper = lotteryUserCredentialMapper;
    }

    /**
     * 按凭证类型和凭证标识查询登录凭证。
     */
    public Optional<LotteryUserCredential> findByCredentialTypeAndIdentifier(String credentialType, String identifier) {
        LotteryUserCredential credential = lotteryUserCredentialMapper.selectOne(Wrappers.<LotteryUserCredential>lambdaQuery()
                .eq(LotteryUserCredential::getCredentialType, credentialType)
                .eq(LotteryUserCredential::getIdentifier, identifier));

        return Optional.ofNullable(credential);
    }

    /**
     * 查询用户绑定的全部登录凭证。
     */
    public List<LotteryUserCredential> findByUserId(Long userId) {
        return lotteryUserCredentialMapper.selectList(Wrappers.<LotteryUserCredential>lambdaQuery()
                .eq(LotteryUserCredential::getUserId, userId)
                .orderByAsc(LotteryUserCredential::getCredentialType)
                .orderByAsc(LotteryUserCredential::getId));
    }

    /**
     * 按用户和凭证类型查询登录凭证。
     */
    public Optional<LotteryUserCredential> findByUserIdAndCredentialType(Long userId, String credentialType) {
        LotteryUserCredential credential = lotteryUserCredentialMapper.selectOne(Wrappers.<LotteryUserCredential>lambdaQuery()
                .eq(LotteryUserCredential::getUserId, userId)
                .eq(LotteryUserCredential::getCredentialType, credentialType));

        return Optional.ofNullable(credential);
    }

    /**
     * 插入登录凭证。
     */
    public int insert(LotteryUserCredential credential) {
        return lotteryUserCredentialMapper.insert(credential);
    }

    /**
     * 按主键更新登录凭证。
     */
    public int updateById(LotteryUserCredential credential) {
        return lotteryUserCredentialMapper.updateById(credential);
    }
}
