package com.hotchpotch.lottery.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.user.entity.LotteryUser;
import com.hotchpotch.lottery.user.entity.LotteryUserCredential;
import com.hotchpotch.lottery.user.entity.LotteryUserRole;
import com.hotchpotch.lottery.user.enums.UserCredentialType;
import com.hotchpotch.lottery.user.enums.UserRole;
import com.hotchpotch.lottery.user.enums.UserStatus;
import com.hotchpotch.lottery.user.record.LoginResponse;
import com.hotchpotch.lottery.user.record.PasswordLoginRequest;
import com.hotchpotch.lottery.user.repository.LotteryUserCredentialRepository;
import com.hotchpotch.lottery.user.repository.LotteryUserRepository;
import com.hotchpotch.lottery.user.repository.LotteryUserRoleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserAuthServiceTest {

    private final LotteryUserCredentialRepository credentialRepository = mock(LotteryUserCredentialRepository.class);
    private final LotteryUserRepository userRepository = mock(LotteryUserRepository.class);
    private final LotteryUserRoleRepository roleRepository = mock(LotteryUserRoleRepository.class);
    private final PasswordHashService passwordHashService = mock(PasswordHashService.class);
    private final UserAuthService service = new UserAuthService(
            credentialRepository,
            userRepository,
            roleRepository,
            passwordHashService);

    @Test
    void loginWithPasswordReturnsUserInfoAndExpandedAdminRoles() {
        LotteryUserCredential credential = credential(10L, "hash");
        when(credentialRepository.findByCredentialTypeAndIdentifier(UserCredentialType.USERNAME.code(), "admin"))
                .thenReturn(Optional.of(credential));
        when(passwordHashService.matches("Admin@123456", "hash")).thenReturn(true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser()));
        when(roleRepository.findByUserId(10L)).thenReturn(List.of(role(UserRole.ADMIN.code())));

        LoginResponse response = service.loginWithPassword(new PasswordLoginRequest("admin", "Admin@123456"));

        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.nickname()).isEqualTo("管理员");
        assertThat(response.avatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(response.roles()).containsExactly(UserRole.USER.code(), UserRole.ADMIN.code());
    }

    @Test
    void loginWithPasswordUsesEmailCredentialTypeAndNormalizesIdentifier() {
        LotteryUserCredential credential = credential(10L, "hash");
        when(credentialRepository.findByCredentialTypeAndIdentifier(UserCredentialType.EMAIL.code(), "user@example.com"))
                .thenReturn(Optional.of(credential));
        when(passwordHashService.matches("Admin@123456", "hash")).thenReturn(true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser()));
        when(roleRepository.findByUserId(10L)).thenReturn(List.of(role(UserRole.USER.code())));

        LoginResponse response = service.loginWithPassword(new PasswordLoginRequest(" USER@Example.COM ", "Admin@123456"));

        assertThat(response.roles()).containsExactly(UserRole.USER.code());
        verify(credentialRepository).findByCredentialTypeAndIdentifier(UserCredentialType.EMAIL.code(), "user@example.com");
    }

    @Test
    void loginWithPasswordUsesPhoneCredentialType() {
        LotteryUserCredential credential = credential(10L, "hash");
        when(credentialRepository.findByCredentialTypeAndIdentifier(UserCredentialType.PHONE.code(), "13800138000"))
                .thenReturn(Optional.of(credential));
        when(passwordHashService.matches("Admin@123456", "hash")).thenReturn(true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser()));
        when(roleRepository.findByUserId(10L)).thenReturn(List.of(role(UserRole.USER.code())));

        service.loginWithPassword(new PasswordLoginRequest("13800138000", "Admin@123456"));

        verify(credentialRepository).findByCredentialTypeAndIdentifier(UserCredentialType.PHONE.code(), "13800138000");
    }

    @Test
    void loginWithPasswordDoesNotTrimRawPassword() {
        LotteryUserCredential credential = credential(10L, "hash");
        when(credentialRepository.findByCredentialTypeAndIdentifier(UserCredentialType.USERNAME.code(), "admin"))
                .thenReturn(Optional.of(credential));
        when(passwordHashService.matches(" Admin@123456 ", "hash")).thenReturn(true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser()));
        when(roleRepository.findByUserId(10L)).thenReturn(List.of(role(UserRole.USER.code())));

        service.loginWithPassword(new PasswordLoginRequest("admin", " Admin@123456 "));

        verify(passwordHashService).matches(" Admin@123456 ", "hash");
    }

    @Test
    void loginWithPasswordRejectsBlankAccountOrPassword() {
        assertThatThrownBy(() -> service.loginWithPassword(new PasswordLoginRequest(" ", "Admin@123456")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
                    assertThat(exception.getMessage()).contains("账号和密码不能为空");
                });
        assertThatThrownBy(() -> service.loginWithPassword(new PasswordLoginRequest("admin", " ")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
                    assertThat(exception.getMessage()).contains("账号和密码不能为空");
                });
    }

    @Test
    void loginWithPasswordRejectsMissingCredentialWithUnifiedMessage() {
        when(credentialRepository.findByCredentialTypeAndIdentifier(UserCredentialType.USERNAME.code(), "missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loginWithPassword(new PasswordLoginRequest("missing", "Admin@123456")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                    assertThat(exception.getMessage()).isEqualTo("账号或密码错误");
                });
        verify(passwordHashService, never()).matches("Admin@123456", "hash");
    }

    @Test
    void loginWithPasswordRejectsWrongPasswordWithUnifiedMessage() {
        LotteryUserCredential credential = credential(10L, "hash");
        when(credentialRepository.findByCredentialTypeAndIdentifier(UserCredentialType.USERNAME.code(), "admin"))
                .thenReturn(Optional.of(credential));
        when(passwordHashService.matches("Wrong@123456", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.loginWithPassword(new PasswordLoginRequest("admin", "Wrong@123456")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                    assertThat(exception.getMessage()).isEqualTo("账号或密码错误");
                });
        verify(userRepository, never()).findById(10L);
    }

    @Test
    void loginWithPasswordRejectsDisabledUser() {
        LotteryUser disabledUser = activeUser();
        disabledUser.setStatus(UserStatus.DISABLED.code());
        LotteryUserCredential credential = credential(10L, "hash");
        when(credentialRepository.findByCredentialTypeAndIdentifier(UserCredentialType.USERNAME.code(), "admin"))
                .thenReturn(Optional.of(credential));
        when(passwordHashService.matches("Admin@123456", "hash")).thenReturn(true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(disabledUser));

        assertThatThrownBy(() -> service.loginWithPassword(new PasswordLoginRequest("admin", "Admin@123456")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN);
                    assertThat(exception.getMessage()).isEqualTo("账号已被禁用");
                });
    }

    private LotteryUserCredential credential(Long userId, String passwordHash) {
        LotteryUserCredential credential = new LotteryUserCredential();
        credential.setUserId(userId);
        credential.setPasswordHash(passwordHash);
        return credential;
    }

    private LotteryUser activeUser() {
        LotteryUser user = new LotteryUser();
        user.setId(10L);
        user.setNickname("管理员");
        user.setAvatarUrl("https://example.com/avatar.png");
        user.setStatus(UserStatus.ACTIVE.code());
        return user;
    }

    private LotteryUserRole role(String roleCode) {
        LotteryUserRole role = new LotteryUserRole();
        role.setRoleCode(roleCode);
        return role;
    }
}
