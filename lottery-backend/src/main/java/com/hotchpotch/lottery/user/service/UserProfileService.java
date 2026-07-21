package com.hotchpotch.lottery.user.service;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.user.entity.LotteryUser;
import com.hotchpotch.lottery.user.entity.LotteryUserCredential;
import com.hotchpotch.lottery.user.enums.UserCredentialType;
import com.hotchpotch.lottery.user.record.UserProfileResponse;
import com.hotchpotch.lottery.user.record.UserProfileUpdateRequest;
import com.hotchpotch.lottery.user.repository.LotteryUserCredentialRepository;
import com.hotchpotch.lottery.user.repository.LotteryUserRepository;
import com.hotchpotch.lottery.user.repository.LotteryUserRoleRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 个人中心资料服务。
 */
@Service
public class UserProfileService {

    private static final int NICKNAME_MAX_LENGTH = 64;
    private static final int AVATAR_URL_MAX_LENGTH = 512;
    private static final int EMAIL_MAX_LENGTH = 128;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Set<String> DEFAULT_AVATAR_URLS = Set.of(
            "/avatars/avatar-01.svg",
            "/avatars/avatar-02.svg",
            "/avatars/avatar-03.svg",
            "/avatars/avatar-04.svg",
            "/avatars/avatar-05.svg",
            "/avatars/avatar-06.svg",
            "/avatars/avatar-07.svg",
            "/avatars/avatar-08.svg");

    private final LotteryUserRepository userRepository;
    private final LotteryUserCredentialRepository credentialRepository;
    private final LotteryUserRoleRepository roleRepository;

    public UserProfileService(
            LotteryUserRepository userRepository,
            LotteryUserCredentialRepository credentialRepository,
            LotteryUserRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * 查询当前用户个人中心资料。
     */
    public UserProfileResponse getProfile(Long userId) {
        return toResponse(findRequiredUser(userId));
    }

    /**
     * 修改当前用户基础资料。
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "个人资料请求体不能为空");
        }

        LotteryUser user = findRequiredUser(userId);
        user.setNickname(normalizeNickname(request.nickname()));
        user.setAvatarUrl(normalizeAvatarUrl(request.avatarUrl()));

        if (request.emailNotificationEnabled() != null) {
            applyEmailNotificationSetting(user, request.emailNotificationEnabled(), request.notificationEmail());
        }
        userRepository.updateById(user);

        return toResponse(user);
    }

    /**
     * 查询用户；不存在时返回资源不存在。
     */
    private LotteryUser findRequiredUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));
    }

    /**
     * 组装个人中心响应。
     */
    private UserProfileResponse toResponse(LotteryUser user) {
        List<LotteryUserCredential> credentials = credentialRepository.findByUserId(user.getId());

        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getStatus(),
                roleRepository.findByUserId(user.getId()).stream()
                        .map(role -> role.getRoleCode())
                        .toList(),
                credentialIdentifier(credentials, UserCredentialType.USERNAME),
                maskPhone(credentialIdentifier(credentials, UserCredentialType.PHONE)),
                maskEmail(credentialIdentifier(credentials, UserCredentialType.EMAIL)),
                Boolean.TRUE.equals(user.getEmailNotificationEnabled()),
                user.getCreateTime(),
                user.getLastLoginTime());
    }

    /**
     * 应用邮箱通知开关；开启时必须存在邮箱凭证或本次提供新邮箱。
     */
    private void applyEmailNotificationSetting(LotteryUser user, Boolean enabled, String notificationEmail) {
        if (!Boolean.TRUE.equals(enabled)) {
            user.setEmailNotificationEnabled(false);
            return;
        }

        LotteryUserCredential emailCredential = ensureEmailCredential(user.getId(), notificationEmail);
        if (emailCredential == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "开启邮箱通知需要先填写邮箱");
        }

        user.setEmailNotificationEnabled(true);
    }

    /**
     * 确保用户已绑定邮箱；本次提供邮箱时会新增或更新 EMAIL 凭证。
     */
    private LotteryUserCredential ensureEmailCredential(Long userId, String notificationEmail) {
        String normalizedEmail = normalizeEmail(notificationEmail);
        if (normalizedEmail == null) {
            return credentialRepository
                    .findByUserIdAndCredentialType(userId, UserCredentialType.EMAIL.code())
                    .orElse(null);
        }

        LotteryUserCredential usedCredential = credentialRepository
                .findByCredentialTypeAndIdentifier(UserCredentialType.EMAIL.code(), normalizedEmail)
                .orElse(null);
        if (usedCredential != null && !userId.equals(usedCredential.getUserId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "邮箱已被其他账号使用");
        }
        if (usedCredential != null) {
            return usedCredential;
        }

        LotteryUserCredential currentCredential = credentialRepository
                .findByUserIdAndCredentialType(userId, UserCredentialType.EMAIL.code())
                .orElse(null);
        if (currentCredential != null) {
            currentCredential.setIdentifier(normalizedEmail);
            currentCredential.setVerified(false);
            credentialRepository.updateById(currentCredential);

            return currentCredential;
        }

        LotteryUserCredential newCredential = new LotteryUserCredential();
        newCredential.setUserId(userId);
        newCredential.setCredentialType(UserCredentialType.EMAIL.code());
        newCredential.setIdentifier(normalizedEmail);
        newCredential.setPasswordHash(null);
        newCredential.setVerified(false);
        credentialRepository.insert(newCredential);

        return newCredential;
    }

    /**
     * 从凭证列表中查找指定类型标识。
     */
    private String credentialIdentifier(List<LotteryUserCredential> credentials, UserCredentialType credentialType) {
        return credentials.stream()
                .filter(credential -> credentialType.code().equals(credential.getCredentialType()))
                .map(LotteryUserCredential::getIdentifier)
                .findFirst()
                .orElse(null);
    }

    /**
     * 校验并规范化昵称。
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
     * 校验并规范化头像地址。
     */
    private String normalizeAvatarUrl(String avatarUrl) {
        String normalizedAvatarUrl = trimToNull(avatarUrl);
        if (normalizedAvatarUrl != null && normalizedAvatarUrl.length() > AVATAR_URL_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "头像地址不能超过 512 个字符");
        }
        if (normalizedAvatarUrl != null && !DEFAULT_AVATAR_URLS.contains(normalizedAvatarUrl)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "头像只能选择系统默认头像");
        }

        return normalizedAvatarUrl;
    }

    /**
     * 校验并规范化通知邮箱。
     */
    private String normalizeEmail(String email) {
        String normalizedEmail = trimToNull(email);
        if (normalizedEmail == null) {
            return null;
        }

        normalizedEmail = normalizedEmail.toLowerCase(Locale.ROOT);
        if (normalizedEmail.length() > EMAIL_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "邮箱不能超过 128 个字符");
        }
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "邮箱格式不正确");
        }

        return normalizedEmail;
    }

    /**
     * 脱敏手机号。
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }

        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 脱敏邮箱。
     */
    private String maskEmail(String email) {
        if (email == null) {
            return null;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        if (localPart.length() == 1) {
            return localPart + "***" + domainPart;
        }

        return localPart.charAt(0) + "****" + localPart.charAt(localPart.length() - 1) + domainPart;
    }

    /**
     * 去除文本前后空白；空字符串统一转为 null。
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
