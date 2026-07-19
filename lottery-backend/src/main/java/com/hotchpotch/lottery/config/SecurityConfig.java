package com.hotchpotch.lottery.config;

import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.user.security.AuthTokenAuthenticationFilter;
import com.hotchpotch.lottery.user.service.AuthSessionService;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 后端安全配置。
 */
@Configuration
public class SecurityConfig {

    /**
     * 配置本地 MVP 联调用的接口访问规则。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthSessionService authSessionService) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/lottery/favorites/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/draws/**",
                                "/api/lottery/**",
                                "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeFailure(response, 401, ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.defaultMessage()))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeFailure(response, 403, ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultMessage())))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .addFilterBefore(
                        new AuthTokenAuthenticationFilter(authSessionService),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 使用 BCrypt 进行密码哈希，哈希结果已包含随机盐和成本参数。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 将 Spring Security 鉴权异常写成统一 API 响应格式。
     */
    private void writeFailure(
            jakarta.servlet.http.HttpServletResponse response,
            int status,
            ErrorCode errorCode,
            String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {"success":false,"code":"%s","message":"%s","data":null}
                """.formatted(errorCode.code(), message));
    }
}
