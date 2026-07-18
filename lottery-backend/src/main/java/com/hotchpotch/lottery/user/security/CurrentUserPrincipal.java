package com.hotchpotch.lottery.user.security;

import java.util.List;

/**
 * Spring Security 中保存的当前登录用户信息。
 *
 * @param userId 用户 ID
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像地址
 * @param roles 用户角色编码列表
 */
public record CurrentUserPrincipal(
        Long userId,
        String nickname,
        String avatarUrl,
        List<String> roles) {
}
