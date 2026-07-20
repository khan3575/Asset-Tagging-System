package com.sil.asset_tagging_system.exception;

import com.sil.asset_tagging_system.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> authenticationErrorHandler(AuthenticationException ex)
    {
        ApiResponse<String> body = new ApiResponse<String>(false, "auth failed", null, LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> invalidLoginRequestErrorHandler(MethodArgumentNotValidException ex)
    {
        ApiResponse<String> body = new ApiResponse<String>(false, "invalid auth", null, LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

}
