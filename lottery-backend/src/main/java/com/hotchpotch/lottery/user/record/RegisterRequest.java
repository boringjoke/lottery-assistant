package com.hotchpotch.lottery.user.record;

/**
 * 用户注册请求。
 *
 * @param username 用户名
 * @param nickname 用户昵称
 * @param password 明文密码
 * @param confirmPassword 确认密码
 */
public record RegisterRequest(
        String username,
        String nickname,
        String password,
        String confirmPassword) {
}
