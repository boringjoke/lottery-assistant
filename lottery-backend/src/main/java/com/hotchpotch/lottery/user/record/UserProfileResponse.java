package com.hotchpotch.lottery.user.record;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人中心资料响应。
 *
 * @param userId 用户 ID
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像地址
 * @param status 用户状态
 * @param roles 用户角色编码列表
 * @param username 用户名
 * @param maskedPhone 脱敏手机号
 * @param maskedEmail 脱敏邮箱
 * @param createTime 创建时间
 * @param lastLoginTime 最近登录时间
 */
public record UserProfileResponse(
        Long userId,
        String nickname,
        String avatarUrl,
        String status,
        List<String> roles,
        String username,
        String maskedPhone,
        String maskedEmail,
        LocalDateTime createTime,
        LocalDateTime lastLoginTime) {
}
