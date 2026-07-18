package com.hotchpotch.lottery.user.record;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户登录会话。
 *
 * @param token 不包含业务含义的随机会话令牌
 * @param userId 用户 ID
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像地址
 * @param roles 用户角色编码列表
 * @param expireTime 会话过期时间
 */
public record AuthSession(
        String token,
        Long userId,
        String nickname,
        String avatarUrl,
        List<String> roles,
        LocalDateTime expireTime) {
}
