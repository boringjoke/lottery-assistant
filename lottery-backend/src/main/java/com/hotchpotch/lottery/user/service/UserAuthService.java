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
import com.hotchpotch.lottery.user.record.RegisterRequest;
import com.hotchpotch.lottery.user.repository.LotteryUserCredentialRepository;
import com.hotchpotch.lottery.user.repository.LotteryUserRepository;
import com.hotchpotch.lottery.user.repository.LotteryUserRoleRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户认证服务。
 */
@Service
public class UserAuthService {

    private static final String PHONE_PATTERN = "^1\\d{10}$";
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,32}$");
    private static final int NICKNAME_MAX_LENGTH = 64;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 64;
    private static final String DEFAULT_AVATAR_URL = "/avatars/avatar-01.svg";

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
     * 注册普通用户账号。
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "注册请求体不能为空");
        }

        String username = normalizeUsername(request.username());
        String nickname = normalizeNickname(request.nickname());
        String password = normalizePassword(request.password(), request.confirmPassword());
        ensureUsernameAvailable(username);

        LocalDateTime now = LocalDateTime.now();
        LotteryUser user = new LotteryUser();
        user.setNickname(nickname);
        user.setAvatarUrl(DEFAULT_AVATAR_URL);
        user.setStatus(UserStatus.ACTIVE.code());
        user.setEmailNotificationEnabled(false);
        user.setLastLoginTime(now);

        try {
            userRepository.insert(user);
            credentialRepository.insert(usernameCredential(user.getId(), username, password));
            roleRepository.insertBatch(List.of(userRole(user.getId(), UserRole.USER.code())));
        } catch (DuplicateKeyException ex) {
            throw usernameAlreadyExists();
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
     * 校验并规范化注册用户名。
     */
    private String normalizeUsername(String username) {
        String normalizedUsername = trimToNull(username);
        if (normalizedUsername == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "用户名不能为空");
        }
        if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "用户名需为 4-32 位字母、数字或下划线");
        }

        return normalizedUsername;
    }

    /**
     * 校验并规范化注册昵称。
     */
    private String normalizeNickname(String nickname) {
        String normalizedNickname = trimToNull(nickname);
        if (normalizedNickname == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "用户昵称不能为空");
        }
        if (normalizedNickname.length() > NICKNAME_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "用户昵称不能超过 64 个字符");
        }

        return normalizedNickname;
    }

    /**
     * 校验注册密码和确认密码。
     */
    private String normalizePassword(String password, String confirmPassword) {
        if (password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "密码不能为空");
        }
        if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "密码长度需为 8-64 位");
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "两次输入的密码不一致");
        }

        return password;
    }

    /**
     * 校验用户名未被占用。
     */
    private void ensureUsernameAvailable(String username) {
        String identifier = identifierOf(UserCredentialType.USERNAME.code(), username);
        if (credentialRepository.findByCredentialTypeAndIdentifier(UserCredentialType.USERNAME.code(), identifier)
                .isPresent()) {
            throw usernameAlreadyExists();
        }
    }

    /**
     * 创建用户名密码凭证。
     */
    private LotteryUserCredential usernameCredential(Long userId, String username, String password) {
        LotteryUserCredential credential = new LotteryUserCredential();
        credential.setUserId(userId);
        credential.setCredentialType(UserCredentialType.USERNAME.code());
        credential.setIdentifier(identifierOf(UserCredentialType.USERNAME.code(), username));
        credential.setPasswordHash(passwordHashService.hash(password));
        credential.setVerified(true);

        return credential;
    }

    /**
     * 创建用户角色记录。
     */
    private LotteryUserRole userRole(Long userId, String roleCode) {
        LotteryUserRole role = new LotteryUserRole();
        role.setUserId(userId);
        role.setRoleCode(roleCode);

        return role;
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

    /**
     * 生成用户名已存在异常。
     */
    private BusinessException usernameAlreadyExists() {
        return new BusinessException(ErrorCode.INVALID_REQUEST, "用户名已存在");
    }
}
