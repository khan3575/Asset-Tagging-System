package com.sil.asset_tagging_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.sil.asset_tagging_system.validation.ValidationConstants;


/**
 * Contains the new password selected by an administrator
 * when resetting an employee password.
 */
public record ResetEmployeePasswordRequest(

        @NotBlank(
                message = "New password is required."
        )
        @Size(
                min = ValidationConstants.PASSWORD_MIN_LENGTH,
                max = ValidationConstants.PASSWORD_MAX_LENGTH,
                message = "New password must be between 8 and 72 characters."
        )
        @Pattern(
                regexp = ValidationConstants.PASSWORD_PATTERN,
                message = "New password must contain uppercase, lowercase, number, and special characters, with no spaces."
        )
        String newPassword

) {
}