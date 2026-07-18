package com.hotchpotch.lottery.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTest {

    @Test
    void passwordEncoderUsesBCrypt() {
        PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();

        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(passwordEncoder.matches("Admin@123456", passwordEncoder.encode("Admin@123456"))).isTrue();
    }
}
