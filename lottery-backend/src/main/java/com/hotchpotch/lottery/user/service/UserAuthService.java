package com.hotchpotch.lottery.user.service;

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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 用户认证服务。
 */
@Service
public class UserAuthService {

    private static final String PHONE_PATTERN = "^1\\d{10}$";

    private final LotteryUserCredentialRepository credentialRepository;
    private final LotteryUserRepository userRepository;
    private final LotteryUserRoleRepository roleRepository;
    private final PasswordHashService passwordHashService;

    public UserAuthService(
            LotteryUserCredentialRepository credentialRepository,
            LotteryUserRepository userRepository,
            LotteryUserRoleRepository roleRepository,
            PasswordHashService passwordHashService) {
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordHashService = passwordHashService;
    }

    /**
     * 使用用户名、手机号或邮箱进行账号密码登录。
     */
    public LoginResponse loginWithPassword(PasswordLoginRequest request) {
        String account = trimToNull(request == null ? null : request.account());
        String password = request == null ? null : request.password();
        if (account == null || password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "账号和密码不能为空");
        }

        String credentialType = credentialTypeOf(account);
        String identifier = identifierOf(credentialType, account);
        LotteryUserCredential credential = credentialRepository
                .findByCredentialTypeAndIdentifier(credentialType, identifier)
                .orElseThrow(this::invalidCredential);

        if (!passwordHashService.matches(password, credential.getPasswordHash())) {
            throw invalidCredential();
        }

        LotteryUser user = userRepository.findById(credential.getUserId())
                .orElseThrow(this::invalidCredential);
        if (!UserStatus.ACTIVE.code().equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        return toLoginResponse(user, roleRepository.findByUserId(user.getId()));
    }

    /**
     * 组装登录成功响应。
     */
    private LoginResponse toLoginResponse(LotteryUser user, List<LotteryUserRole> roles) {
        return new LoginResponse(
                user.getId(),
                user.getNickname(),
                user.getAvatarUrl(),
                expandRoles(roles));
    }

    /**
     * 展开角色列表；管理员自动拥有普通用户能力。
     */
    private List<String> expandRoles(List<LotteryUserRole> roles) {
        Set<String> roleCodes = new LinkedHashSet<>();
        boolean hasAdmin = roles.stream()
                .anyMatch(role -> UserRole.ADMIN.code().equals(role.getRoleCode()));
        if (hasAdmin) {
            roleCodes.add(UserRole.USER.code());
        }
        roles.stream()
                .map(LotteryUserRole::getRoleCode)
                .filter(roleCode -> roleCode != null && !roleCode.isBlank())
                .forEach(roleCodes::add);

        return new ArrayList<>(roleCodes);
    }

    /**
     * 根据账号格式判断登录凭证类型。
     */
    private String credentialTypeOf(String account) {
        if (account.contains("@")) {
            return UserCredentialType.EMAIL.code();
        }
        if (account.matches(PHONE_PATTERN)) {
            return UserCredentialType.PHONE.code();
        }

        return UserCredentialType.USERNAME.code();
    }

    /**
     * 生成用于查询凭证表的标识；邮箱统一转小写。
     */
    private String identifierOf(String credentialType, String account) {
        if (UserCredentialType.EMAIL.code().equals(credentialType)) {
            return account.toLowerCase();
        }

        return account;
    }

    /**
     * 去除账号前后空白；空字符串统一转为 null。
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * 生成统一的账号密码错误异常，避免泄露账号是否存在。
     */
    private BusinessException invalidCredential() {
        return new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
    }
}
