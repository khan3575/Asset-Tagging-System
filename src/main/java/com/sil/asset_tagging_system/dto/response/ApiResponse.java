package com.sil.asset_tagging_system.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;


/**
 * Standard response wrapper used by REST API endpoints.
 *
 * This class provides a consistent JSON structure
 * for successful and unsuccessful API responses.
 *
 * @param success   indicates whether the operation succeeded
 * @param message   human-readable response message
 * @param data      response payload
 * @param timestamp date and time when the response was created
 * @param <T>       response-data type
 */
@JsonInclude(
        JsonInclude.Include.ALWAYS
)
public record ApiResponse<T>(

        boolean success,

        String message,

        T data,

        LocalDateTime timestamp

) {


    // ========================================================
    // SUCCESS RESPONSE WITH DATA
    // ========================================================

    /**
     * Creates a successful API response containing data.
     *
     * Example:
     *
     * ApiResponse.success(
     *     "Employee created successfully.",
     *     employeeResponse
     * );
     *
     * @param message success message
     * @param data    response data
     * @param <T>     response-data type
     * @return successful API response
     */
    public static <T> ApiResponse<T> success(

            String message,

            T data
    ) {

        return new ApiResponse<>(

                true,

                message,

                data,

                LocalDateTime.now()
        );
    }


    // ========================================================
    // SUCCESS RESPONSE WITHOUT DATA
    // ========================================================

    /**
     * Creates a successful API response without data.
     *
     * Example:
     *
     * ApiResponse.success(
     *     "Employee password reset successfully."
     * );
     *
     * @param message success message
     * @return successful API response
     */
    public static ApiResponse<Void> success(
            String message
    ) {

        return new ApiResponse<>(

                true,

                message,

                null,

                LocalDateTime.now()
        );
    }


    // ========================================================
    // FAILURE RESPONSE WITHOUT DATA
    // ========================================================

    /**
     * Creates an unsuccessful API response
     * without additional response data.
     *
     * Example:
     *
     * ApiResponse.failure(
     *     "Employee was not found."
     * );
     *
     * @param message failure message
     * @return unsuccessful API response
     */
    public static ApiResponse<Void> failure(
            String message
    ) {

        return new ApiResponse<>(

                false,

                message,

                null,

                LocalDateTime.now()
        );
    }


    // ========================================================
    // FAILURE RESPONSE WITH DATA
    // ========================================================

    /**
     * Creates an unsuccessful API response
     * containing additional error information.
     *
     * This can later be used for validation errors.
     *
     * @param message failure message
     * @param data    additional error information
     * @param <T>     error-data type
     * @return unsuccessful API response
     */
    public static <T> ApiResponse<T> failure(

            String message,

            T data
    ) {

        return new ApiResponse<>(

                false,

                message,

                data,

                LocalDateTime.now()
        );
    }

}