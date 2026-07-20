package com.sil.asset_tagging_system.dto.response;

import org.junit.jupiter.api.Test;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Assertions.within;

public class ApiResponseTest {
    @Test
    void buildSuccessResponse(){
        ApiResponse<String> response = ApiResponse.success("ok", "hello");
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("ok");
        assertThat(response.data()).isEqualTo("hello");
        assertThat(response.timestamp()).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS));

    }

    @Test
    void buildFailureResponse()
    {
        // here one test should only test one thing. this 4 data is connected to only the Api response so testing the 4 data at once isnt bad practise but its best to remember that if the first assert fails the 3 bellow wont even be activated. once the first assert is fixed then the 2nd one will be tested.
        ApiResponse<String> response = ApiResponse.failure("ok");
        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("ok");
        assertThat(response.data()).isNull();
        assertThat(response.timestamp()).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS));
    }

}
