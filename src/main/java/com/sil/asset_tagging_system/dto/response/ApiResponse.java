package com.sil.asset_tagging_system.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public record ApiResponse<T>(

        @NotNull
        Boolean success,

        @NotNull
        @NotBlank
        String message,

        T data,
        LocalDateTime timestamp
)
{
    public static <T> ApiResponse<T> success(String message, T data)
    {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }
    public static <T> ApiResponse<T> failure(String message)
    {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
    public static <T> ApiResponse<T> failure(String message, T data)
    {
        return new ApiResponse<>(false, message, data, LocalDateTime.now());
    }
    // Inside your ApiResponse.java class
    public ResponseEntity<ApiResponse<T>> toResponseEntity(HttpStatus status) {
        return new ResponseEntity<>(this, status);
    }

}
