package com.hotchpotch.lottery.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户密码哈希服务。
 */
@Service
public class PasswordHashService {

    private final PasswordEncoder passwordEncoder;

    public PasswordHashService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 生成密码哈希。
     */
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 校验明文密码和密码哈希是否匹配。
     */
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordHash != null && passwordEncoder.matches(rawPassword, passwordHash);
    }
}
