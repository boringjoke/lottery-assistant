package com.hotchpotch.lottery.common.exception;

/**
 * 后端稳定业务错误码。
 */
public enum ErrorCode {
    INVALID_REQUEST("INVALID_REQUEST", "请求参数不合法"),
    UNAUTHORIZED("UNAUTHORIZED", "请先登录"),
    FORBIDDEN("FORBIDDEN", "无权限访问"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "资源不存在"),
    UPSTREAM_SERVICE_ERROR("UPSTREAM_SERVICE_ERROR", "上游服务异常"),
    INTERNAL_ERROR("INTERNAL_ERROR", "系统异常");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
