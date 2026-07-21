package com.sil.asset_tagging_system.exception;

import com.sil.asset_tagging_system.dto.response.ApiResponse;


import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;

import org.springframework.web.bind.MethodArgumentNotValidException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Converts application exceptions into
 * consistent JSON API responses.
 * <p>
 * This handler prevents REST controllers from
 * returning Spring Boot's default error response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {


    // ========================================================
    // LOGGER
    // ========================================================

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );


    // ========================================================
    // RESOURCE NOT FOUND
    // ========================================================

    /**
     * Handles missing application resources.
     * <p>
     * Examples:
     * <p>
     * - employee not found
     * - department not found
     * - role not found
     */

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException exception) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(exception.getMessage()));
        }

    @ExceptionHandler(
            ResourceNotFoundException.class
    )
    public ResponseEntity<ApiResponse<Void>>
    handleResourceNotFound(

            ResourceNotFoundException exception
    ) {

        return ResponseEntity
                .status(
                        HttpStatus.NOT_FOUND
                )
                .body(
                        ApiResponse.failure(
                                exception.getMessage()
                        )
                );
    }


    // ========================================================
    // DUPLICATE RESOURCE
    // ========================================================

    /**
     * Handles duplicate application resources.
     * <p>
     * Example:
     * <p>
     * An account already exists using
     * the requested email address.
     */
    @ExceptionHandler(
            DuplicateResourceException.class
    )
    public ResponseEntity<ApiResponse<Void>>
    handleDuplicateResource(

            DuplicateResourceException exception
    ) {

        return ResponseEntity
                .status(
                        HttpStatus.CONFLICT
                )
                .body(
                        ApiResponse.failure(
                                exception.getMessage()
                        )
                );
    }


    // ========================================================
    // BUSINESS-RULE VIOLATION
    // ========================================================

    /**
     * Handles requests that violate an
     * application business rule.
     */
    @ExceptionHandler(
            BusinessRuleException.class
    )
    public ResponseEntity<ApiResponse<Void>>
    handleBusinessRuleViolation(

            BusinessRuleException exception
    ) {

        return ResponseEntity
                .status(
                        HttpStatus.UNPROCESSABLE_CONTENT
                )
                .body(
                        ApiResponse.failure(
                                exception.getMessage()
                        )
                );
    }


    // ========================================================
    // REQUEST-BODY VALIDATION
    // ========================================================

    /**
     * Handles validation failures from
     * request DTOs annotated using @Valid.
     * <p>
     * Examples:
     * <p>
     * - blank first name
     * - invalid email
     * - weak password
     * - missing department ID
     */
    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<
            ApiResponse<Map<String, String>>
            >
    handleRequestBodyValidation(

            MethodArgumentNotValidException exception
    ) {

        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        exception
                .getBindingResult()
                .getAllErrors()
                .forEach(

                        error -> {

                            String fieldName =
                                    error instanceof FieldError
                                            ? (
                                            (FieldError) error
                                    ).getField()
                                            : error.getObjectName();

                            String errorMessage =
                                    error.getDefaultMessage();

                            validationErrors.putIfAbsent(
                                    fieldName,
                                    errorMessage
                            );
                        }
                );

        return ResponseEntity
                .badRequest()
                .body(
                        ApiResponse.failure(
                                "Request validation failed.",
                                validationErrors
                        )
                );
    }


    // ========================================================
    // PATH-VARIABLE AND REQUEST-PARAMETER VALIDATION
    // ========================================================

    /**
     * Handles validation failures from:
     * <p>
     * - @PathVariable
     * - @RequestParam
     * <p>
     * Examples:
     * <p>
     * employeeId = 0
     * page = -1
     * size = 0
     */
    @ExceptionHandler(
            ConstraintViolationException.class
    )
    public ResponseEntity<
            ApiResponse<Map<String, String>>
            >
    handleConstraintViolation(

            ConstraintViolationException exception
    ) {

        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        for (
                ConstraintViolation<?> violation
                : exception.getConstraintViolations()
        ) {

            String propertyPath =
                    violation
                            .getPropertyPath()
                            .toString();

            String parameterName =
                    extractFinalPropertyName(
                            propertyPath
                    );

            validationErrors.putIfAbsent(

                    parameterName,

                    violation.getMessage()
            );
        }

        return ResponseEntity
                .badRequest()
                .body(
                        ApiResponse.failure(
                                "Request validation failed.",
                                validationErrors
                        )
                );
    }


        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException exception) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.failure(exception.getMessage()));
        }

    // ========================================================
    // INVALID PARAMETER TYPE
    // ========================================================

    /**
     * Handles invalid request-parameter and
     * path-variable data types.
     * <p>
     * Example:
     * <p>
     * /employees/abc
     * <p>
     * when employeeId must be a Long.
     */
    @ExceptionHandler(
            MethodArgumentTypeMismatchException.class
    )
    public ResponseEntity<ApiResponse<Void>>
    handleTypeMismatch(

            MethodArgumentTypeMismatchException exception
    ) {

        String parameterName =
                exception.getName();

        String message =
                "Invalid value supplied for parameter '%s'."
                        .formatted(
                                parameterName
                        );

        return ResponseEntity
                .badRequest()
                .body(
                        ApiResponse.failure(
                                message
                        )
                );
    }


    // ========================================================
    // MALFORMED OR INVALID JSON
    // ========================================================

    /**
     * Handles request bodies that contain:
     * <p>
     * - malformed JSON
     * - invalid Boolean values
     * - invalid enum values
     * - incompatible field types
     */
    @ExceptionHandler(
            HttpMessageNotReadableException.class
    )
    public ResponseEntity<ApiResponse<Void>>
    handleUnreadableRequestBody(

            HttpMessageNotReadableException exception
    ) {

        LOGGER.debug(
                "Request body could not be read.",
                exception
        );

        return ResponseEntity
                .badRequest()
                .body(
                        ApiResponse.failure(
                                "The request body is missing, malformed, or contains invalid values."
                        )
                );
    }


    // ========================================================
    // DATABASE CONSTRAINT VIOLATION
    // ========================================================

    /**
     * Handles database constraints that may still
     * occur after application-level validation.
     * <p>
     * The database remains the final protection
     * against duplicate or invalid data.
     */
    @ExceptionHandler(
            DataIntegrityViolationException.class
    )
    public ResponseEntity<ApiResponse<Void>>
    handleDataIntegrityViolation(

            DataIntegrityViolationException exception
    ) {

        LOGGER.warn(
                "A database integrity constraint was violated.",
                exception
        );

        return ResponseEntity
                .status(
                        HttpStatus.CONFLICT
                )
                .body(
                        ApiResponse.failure(
                                "The requested operation conflicts with existing data."
                        )
                );
    }


    // ========================================================
    // UNEXPECTED APPLICATION ERROR
    // ========================================================

    /**
     * Handles unexpected exceptions.
     * <p>
     * Internal exception details are logged but
     * are never exposed to API clients.
     */
    @ExceptionHandler(
            Exception.class
    )
    public ResponseEntity<ApiResponse<Void>>
    handleUnexpectedException(

            Exception exception
    ) {

        LOGGER.error(
                "An unexpected application error occurred.",
                exception
        );

        return ResponseEntity
                .status(
                        HttpStatus.INTERNAL_SERVER_ERROR
                )
                .body(
                        ApiResponse.failure(
                                "An unexpected server error occurred."
                        )
                );
    }


    // ========================================================
    // PROPERTY-PATH HELPER
    // ========================================================

    /**
     * Extracts the final parameter name from a
     * validation property path.
     * <p>
     * Example:
     * <p>
     * getEmployeeById.employeeId
     * <p>
     * becomes:
     * <p>
     * employeeId
     */
    private String extractFinalPropertyName(
            String propertyPath
    ) {

        int finalDotIndex =
                propertyPath.lastIndexOf(
                        '.'
                );

        if (
                finalDotIndex >= 0
                        && finalDotIndex
                        < propertyPath.length() - 1
        ) {

            return propertyPath.substring(
                    finalDotIndex + 1
            );
        }

        return propertyPath;
    }

}

