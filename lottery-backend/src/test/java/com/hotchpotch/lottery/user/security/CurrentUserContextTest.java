package com.hotchpotch.lottery.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserContextTest {

    private final CurrentUserContext currentUserContext = new CurrentUserContext();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证可以从 Spring Security 上下文读取当前登录用户 ID。
     */
    @Test
    void requireUserIdReturnsCurrentPrincipalUserId() {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(
                10L,
                "测试用户",
                null,
                List.of("USER"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                "token",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThat(currentUserContext.requireUserId()).isEqualTo(10L);
        assertThat(currentUserContext.requireCurrentUser()).isSameAs(principal);
        assertThat(currentUserContext.requireToken()).isEqualTo("token");
    }

    /**
     * 验证未登录时抛出统一未授权异常。
     */
    @Test
    void requireCurrentUserRejectsMissingAuthentication() {
        assertThatThrownBy(currentUserContext::requireCurrentUser)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThatThrownBy(currentUserContext::requireToken)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 验证匿名用户不会被当作业务登录用户。
     */
    @Test
    void requireCurrentUserRejectsAnonymousPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "anonymous",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(currentUserContext::requireCurrentUser)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
