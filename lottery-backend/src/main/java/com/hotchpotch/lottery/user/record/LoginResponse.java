package com.hotchpotch.lottery.user.record;

import java.util.List;

/**
 * 登录成功响应。
 *
 * @param userId 用户 ID
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像地址
 * @param roles 用户角色编码列表
 */
public record LoginResponse(
        Long userId,
        String nickname,
        String avatarUrl,
        List<String> roles) {
}
