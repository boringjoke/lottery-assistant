package com.hotchpotch.lottery.common.response;

import com.hotchpotch.lottery.common.exception.ErrorCode;

/**
 * API 统一响应结构。
 *
 * @param <T> 响应数据类型
 * @param success 请求是否成功
 * @param code 响应业务编码
 * @param message 响应提示消息
 * @param data 响应数据
 */
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", "success", data);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return failure(errorCode, errorCode.defaultMessage());
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.code(), message, null);
    }
}
