package com.hotchpotch.lottery.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotchpotch.lottery.user.entity.LotteryUser;
import com.hotchpotch.lottery.user.entity.LotteryUserCredential;
import com.hotchpotch.lottery.user.entity.LotteryUserOAuthAccount;
import com.hotchpotch.lottery.user.entity.LotteryUserRole;
import com.hotchpotch.lottery.user.mapper.LotteryUserCredentialMapper;
import com.hotchpotch.lottery.user.mapper.LotteryUserMapper;
import com.hotchpotch.lottery.user.mapper.LotteryUserOAuthAccountMapper;
import com.hotchpotch.lottery.user.mapper.LotteryUserRoleMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class LotteryUserRepositoryTest {

    @Test
    void mappersExtendMybatisPlusBaseMapper() {
        assertThat(BaseMapper.class).isAssignableFrom(LotteryUserMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(LotteryUserCredentialMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(LotteryUserOAuthAccountMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(LotteryUserRoleMapper.class);
    }

    @Test
    void userRepositoryFindsUserById() {
        LotteryUserMapper mapper = mock(LotteryUserMapper.class);
        LotteryUser user = new LotteryUser();
        when(mapper.selectById(10L)).thenReturn(user);

        LotteryUserRepository repository = new LotteryUserRepository(mapper);

        assertThat(repository.findById(10L)).containsSame(user);
        verify(mapper).selectById(10L);
    }

    @Test
    void userRepositoryDelegatesInsertAndUpdateById() {
        LotteryUserMapper mapper = mock(LotteryUserMapper.class);
        LotteryUser user = new LotteryUser();
        when(mapper.insert(user)).thenReturn(1);
        when(mapper.updateById(user)).thenReturn(1);

        LotteryUserRepository repository = new LotteryUserRepository(mapper);

        assertThat(repository.insert(user)).isEqualTo(1);
        assertThat(repository.updateById(user)).isEqualTo(1);
    }

    @Test
    void userCredentialRepositoryFindsCredentialByTypeAndIdentifier() {
        LotteryUserCredentialMapper mapper = mock(LotteryUserCredentialMapper.class);
        LotteryUserCredential credential = new LotteryUserCredential();
        when(mapper.selectOne(anyUserCredentialWrapper())).thenReturn(credential);

        LotteryUserCredentialRepository repository = new LotteryUserCredentialRepository(mapper);

        assertThat(repository.findByCredentialTypeAndIdentifier("EMAIL", "user@example.com")).containsSame(credential);
        verify(mapper).selectOne(anyUserCredentialWrapper());
    }

    @Test
    void userCredentialRepositoryFindsCredentialsByUserIdAndDelegatesWrites() {
        LotteryUserCredentialMapper mapper = mock(LotteryUserCredentialMapper.class);
        LotteryUserCredential credential = new LotteryUserCredential();
        when(mapper.selectList(anyUserCredentialWrapper())).thenReturn(List.of(credential));
        when(mapper.insert(credential)).thenReturn(1);
        when(mapper.updateById(credential)).thenReturn(1);

        LotteryUserCredentialRepository repository = new LotteryUserCredentialRepository(mapper);

        assertThat(repository.findByUserId(10L)).containsExactly(credential);
        assertThat(repository.insert(credential)).isEqualTo(1);
        assertThat(repository.updateById(credential)).isEqualTo(1);
        verify(mapper).selectList(anyUserCredentialWrapper());
    }

    @Test
    void userOAuthAccountRepositoryFindsAccountByProviderAndOpenId() {
        LotteryUserOAuthAccountMapper mapper = mock(LotteryUserOAuthAccountMapper.class);
        LotteryUserOAuthAccount account = new LotteryUserOAuthAccount();
        when(mapper.selectOne(anyUserOAuthAccountWrapper())).thenReturn(account);

        LotteryUserOAuthAccountRepository repository = new LotteryUserOAuthAccountRepository(mapper);

        assertThat(repository.findByProviderAndOpenId("WECHAT_MINI_PROGRAM", "openid-001")).containsSame(account);
        verify(mapper).selectOne(anyUserOAuthAccountWrapper());
    }

    @Test
    void userOAuthAccountRepositoryFindsAccountsByUserIdAndUnionIdAndDelegatesWrites() {
        LotteryUserOAuthAccountMapper mapper = mock(LotteryUserOAuthAccountMapper.class);
        LotteryUserOAuthAccount account = new LotteryUserOAuthAccount();
        when(mapper.selectList(anyUserOAuthAccountWrapper())).thenReturn(List.of(account));
        when(mapper.insert(account)).thenReturn(1);
        when(mapper.updateById(account)).thenReturn(1);

        LotteryUserOAuthAccountRepository repository = new LotteryUserOAuthAccountRepository(mapper);

        assertThat(repository.findByUserId(10L)).containsExactly(account);
        assertThat(repository.findByUnionId("union-001")).containsExactly(account);
        assertThat(repository.insert(account)).isEqualTo(1);
        assertThat(repository.updateById(account)).isEqualTo(1);
        verify(mapper, org.mockito.Mockito.times(2)).selectList(anyUserOAuthAccountWrapper());
    }

    @Test
    void userRoleRepositoryFindsRolesByUserIdAndInsertsBatch() {
        LotteryUserRoleMapper mapper = mock(LotteryUserRoleMapper.class);
        LotteryUserRole role = new LotteryUserRole();
        when(mapper.selectList(anyUserRoleWrapper())).thenReturn(List.of(role));
        when(mapper.insert(role)).thenReturn(1);

        LotteryUserRoleRepository repository = new LotteryUserRoleRepository(mapper);

        assertThat(repository.findByUserId(10L)).containsExactly(role);
        assertThat(repository.insertBatch(List.of(role))).isEqualTo(1);
        verify(mapper).selectList(anyUserRoleWrapper());
        verify(mapper).insert(role);
    }

    @Test
    void userRoleRepositoryDeletesRolesByUserId() {
        LotteryUserRoleMapper mapper = mock(LotteryUserRoleMapper.class);
        when(mapper.delete(anyUserRoleWrapper())).thenReturn(2);

        LotteryUserRoleRepository repository = new LotteryUserRoleRepository(mapper);

        assertThat(repository.deleteByUserId(10L)).isEqualTo(2);
        verify(mapper).delete(anyUserRoleWrapper());
    }

    private Wrapper<LotteryUser> anyUserWrapper() {
        return any();
    }

    private Wrapper<LotteryUserCredential> anyUserCredentialWrapper() {
        return any();
    }

    private Wrapper<LotteryUserOAuthAccount> anyUserOAuthAccountWrapper() {
        return any();
    }

    private Wrapper<LotteryUserRole> anyUserRoleWrapper() {
        return any();
    }
}
