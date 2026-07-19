package com.hotchpotch.lottery.user.security;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 当前登录用户上下文读取器。
 */
@Component
public class CurrentUserContext {

    /**
     * 读取当前登录用户 ID；未登录时返回统一未授权异常。
     */
    public Long requireUserId() {
        return requireCurrentUser().userId();
    }

    /**
     * 读取当前登录用户完整信息；未登录时返回统一未授权异常。
     */
    public CurrentUserPrincipal requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CurrentUserPrincipal currentUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return currentUser;
    }

    /**
     * 读取当前会话 token；用于资料变更后同步刷新服务端会话缓存。
     */
    public String requireToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getCredentials() instanceof String token)
                || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return token;
    }
}
