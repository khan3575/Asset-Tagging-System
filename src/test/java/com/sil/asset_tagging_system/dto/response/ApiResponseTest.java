package com.sil.asset_tagging_system.dto.response;

import org.junit.jupiter.api.Test;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ApiResponseTest {
    @Test
    void buildSuccessResponse(){
        ApiResponse<String> response = ApiResponse.success("ok", "hello");
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("ok");
        assertThat(response.data()).isEqualTo("hello");
    }

    @Test
    void buildFailureResponse()
    {
        ApiResponse<String> response = ApiResponse.failure("ok");
        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("ok");
        assertThat(response.data()).isNull();
    }

}
