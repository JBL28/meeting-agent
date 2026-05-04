package com.ssafy.meeting.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void successContainsDataWithoutError() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("ok");
        assertThat(response.getError()).isNull();
    }

    @Test
    void failureContainsErrorWithoutData() {
        ApiResponse<Void> response = ApiResponse.failure("VALIDATION_ERROR", "bad request");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getError().getCode()).isEqualTo("VALIDATION_ERROR");
    }
}
