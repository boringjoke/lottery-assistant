package com.hotchpotch.lottery.user.record;

/**
 * 修改个人中心资料请求。
 *
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像地址
 */
public record UserProfileUpdateRequest(
        String nickname,
        String avatarUrl) {
}
