package com.hotchpotch.lottery.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashServiceTest {

    private final PasswordHashService passwordHashService = new PasswordHashService(new BCryptPasswordEncoder());

    @Test
    void hashUsesRandomSaltForSameRawPassword() {
        String firstHash = passwordHashService.hash("Admin@123456");
        String secondHash = passwordHashService.hash("Admin@123456");

        assertThat(firstHash).startsWith("$2");
        assertThat(secondHash).startsWith("$2");
        assertThat(firstHash).isNotEqualTo(secondHash);
    }

    @Test
    void matchesReturnsTrueForOriginalPassword() {
        String passwordHash = passwordHashService.hash("Admin@123456");

        assertThat(passwordHashService.matches("Admin@123456", passwordHash)).isTrue();
    }

    @Test
    void matchesReturnsFalseForWrongPassword() {
        String passwordHash = passwordHashService.hash("Admin@123456");

        assertThat(passwordHashService.matches("Wrong@123456", passwordHash)).isFalse();
    }

    @Test
    void matchesReturnsFalseWhenPasswordHashIsNull() {
        assertThat(passwordHashService.matches("Admin@123456", null)).isFalse();
    }
}
