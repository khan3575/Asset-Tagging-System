package com.sil.asset_tagging_system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.sil.asset_tagging_system.validation.ValidationConstants;


/**
 * Contains editable employee profile information.
 *
 * Password, account status, and roles are intentionally
 * excluded because they are managed by separate operations.
 */
public record UpdateEmployeeRequest(

        @NotBlank(
                message = "First name is required."
        )
        @Size(
                max = ValidationConstants.NAME_MAX_LENGTH,
                message = "First name cannot exceed 60 characters."
        )
        @Pattern(
                regexp = ValidationConstants.NAME_PATTERN,
                message = "First name contains invalid characters."
        )
        String firstName,


        @NotBlank(
                message = "Last name is required."
        )
        @Size(
                max = ValidationConstants.NAME_MAX_LENGTH,
                message = "Last name cannot exceed 60 characters."
        )
        @Pattern(
                regexp = ValidationConstants.NAME_PATTERN,
                message = "Last name contains invalid characters."
        )
        String lastName,


        @NotBlank(
                message = "Email address is required."
        )
        @Email(
                message = "Enter a valid email address."
        )
        @Size(
                max = ValidationConstants.EMAIL_MAX_LENGTH,
                message = "Email address cannot exceed 100 characters."
        )
        String email,


        @NotNull(
                message = "Department ID is required."
        )
        @Positive(
                message = "Department ID must be greater than zero."
        )
        Long departmentId

) {
}