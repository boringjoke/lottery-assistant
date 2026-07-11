package com.hotchpotch.lottery.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.common.response.ApiResponse;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void successWrapsDataWithSuccessCode() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo("SUCCESS");
        assertThat(response.message()).isEqualTo("success");
        assertThat(response.data()).isEqualTo("ok");
    }

    @Test
    void failureWrapsStableErrorCodeAndMessage() {
        ApiResponse<Void> response = ApiResponse.failure(ErrorCode.INVALID_REQUEST, "参数错误");

        assertThat(response.success()).isFalse();
        assertThat(response.code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.message()).isEqualTo("参数错误");
        assertThat(response.data()).isNull();
    }
}
