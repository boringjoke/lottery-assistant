package com.hotchpotch.lottery.user.record;

/**
 * 账号密码登录请求。
 *
 * @param account 用户名、手机号或邮箱
 * @param password 明文密码
 */
public record PasswordLoginRequest(String account, String password) {
}
