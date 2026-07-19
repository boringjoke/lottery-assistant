package com.hotchpotch.lottery.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.user.entity.LotteryUser;
import com.hotchpotch.lottery.user.entity.LotteryUserCredential;
import com.hotchpotch.lottery.user.entity.LotteryUserRole;
import com.hotchpotch.lottery.user.record.UserProfileUpdateRequest;
import com.hotchpotch.lottery.user.repository.LotteryUserCredentialRepository;
import com.hotchpotch.lottery.user.repository.LotteryUserRepository;
import com.hotchpotch.lottery.user.repository.LotteryUserRoleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private LotteryUserRepository userRepository;

    @Mock
    private LotteryUserCredentialRepository credentialRepository;

    @Mock
    private LotteryUserRoleRepository roleRepository;

    /**
     * 验证个人中心资料会聚合用户、角色和脱敏凭证。
     */
    @Test
    void getProfileReturnsUserRolesAndMaskedCredentials() {
        LotteryUser user = user();
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(credentialRepository.findByUserId(10L)).thenReturn(List.of(
                credential("USERNAME", "normal"),
                credential("PHONE", "13800138000"),
                credential("EMAIL", "normal@example.com")));
        when(roleRepository.findByUserId(10L)).thenReturn(List.of(role("USER")));
        UserProfileService service = service();

        var response = service.getProfile(10L);

        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.nickname()).isEqualTo("普通用户");
        assertThat(response.username()).isEqualTo("normal");
        assertThat(response.maskedPhone()).isEqualTo("138****8000");
        assertThat(response.maskedEmail()).isEqualTo("n****l@example.com");
        assertThat(response.roles()).containsExactly("USER");
        assertThat(response.createTime()).isEqualTo(LocalDateTime.of(2026, 7, 18, 10, 0));
        assertThat(response.lastLoginTime()).isEqualTo(LocalDateTime.of(2026, 7, 18, 12, 0));
    }

    /**
     * 验证修改资料只更新昵称和头像。
     */
    @Test
    void updateProfileChangesNicknameAndAvatarUrl() {
        LotteryUser user = user();
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(credentialRepository.findByUserId(10L)).thenReturn(List.of(credential("USERNAME", "normal")));
        when(roleRepository.findByUserId(10L)).thenReturn(List.of(role("USER")));
        UserProfileService service = service();

        var response = service.updateProfile(10L, new UserProfileUpdateRequest(
                " 新昵称 ",
                " /avatars/avatar-02.svg "));

        assertThat(response.nickname()).isEqualTo("新昵称");
        assertThat(response.avatarUrl()).isEqualTo("/avatars/avatar-02.svg");
        assertThat(user.getNickname()).isEqualTo("新昵称");
        assertThat(user.getAvatarUrl()).isEqualTo("/avatars/avatar-02.svg");
        verify(userRepository).updateById(user);
    }

    /**
     * 验证昵称不能为空。
     */
    @Test
    void updateProfileRejectsBlankNickname() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user()));
        UserProfileService service = service();

        assertThatThrownBy(() -> service.updateProfile(10L, new UserProfileUpdateRequest(" ", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    /**
     * 验证头像只能使用系统提供的默认头像。
     */
    @Test
    void updateProfileRejectsUnsupportedAvatarUrl() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user()));
        UserProfileService service = service();

        assertThatThrownBy(() -> service.updateProfile(10L, new UserProfileUpdateRequest(
                "新昵称",
                "https://example.com/avatar.png")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    /**
     * 验证用户不存在时返回资源不存在。
     */
    @Test
    void getProfileRejectsMissingUser() {
        when(userRepository.findById(10L)).thenReturn(Optional.empty());
        UserProfileService service = service();

        assertThatThrownBy(() -> service.getProfile(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    private UserProfileService service() {
        return new UserProfileService(userRepository, credentialRepository, roleRepository);
    }

    private LotteryUser user() {
        LotteryUser user = new LotteryUser();
        user.setId(10L);
        user.setNickname("普通用户");
        user.setAvatarUrl(null);
        user.setStatus("ACTIVE");
        user.setCreateTime(LocalDateTime.of(2026, 7, 18, 10, 0));
        user.setLastLoginTime(LocalDateTime.of(2026, 7, 18, 12, 0));
        return user;
    }

    private LotteryUserCredential credential(String credentialType, String identifier) {
        LotteryUserCredential credential = new LotteryUserCredential();
        credential.setUserId(10L);
        credential.setCredentialType(credentialType);
        credential.setIdentifier(identifier);
        credential.setVerified(true);
        return credential;
    }

    private LotteryUserRole role(String roleCode) {
        LotteryUserRole role = new LotteryUserRole();
        role.setUserId(10L);
        role.setRoleCode(roleCode);
        return role;
    }
}
