package com.hotchpotch.lottery.user.record;

/**
 * 修改个人中心资料请求。
 *
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像地址
 * @param emailNotificationEnabled 是否开启邮箱通知；为空表示不修改
 * @param notificationEmail 开启邮箱通知且当前未绑定邮箱时填写的通知邮箱
 */
public record UserProfileUpdateRequest(
        String nickname,
        String avatarUrl,
        Boolean emailNotificationEnabled,
        String notificationEmail) {
}
