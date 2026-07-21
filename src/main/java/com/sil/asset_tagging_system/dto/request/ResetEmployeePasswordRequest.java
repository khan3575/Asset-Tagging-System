package com.sil.asset_tagging_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


/**
 * Contains the new password selected by an administrator
 * when resetting an employee password.
 */
public record ResetEmployeePasswordRequest(

        @NotBlank(
                message = "New password is required."
        )
        @Size(
                min = 8,
                max = 12,
                message = "New password must contain between 8 and 12 characters."
        )
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
                message = "New password must contain uppercase, lowercase, number, and special characters, with no spaces."
        )
        String newPassword

) {
}