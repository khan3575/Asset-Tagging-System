package com.sil.asset_tagging_system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;


public record CreateEmployeeRequest(

        @NotBlank(
                message = "First name is required."
        )
        @Size(
                max = 60,
                message = "First name cannot exceed 60 characters."
        )
        @Pattern(
                regexp = "^\\p{L}[\\p{L} .'-]*$",
                message = "First name contains invalid characters."
        )
        String firstName,


        @NotBlank(
                message = "Last name is required."
        )
        @Size(
                max = 60,
                message = "Last name cannot exceed 60 characters."
        )
        @Pattern(
                regexp = "^\\p{L}[\\p{L} .'-]*$",
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
                max = 100,
                message = "Email address cannot exceed 100 characters."
        )
        String email,


        @NotBlank(
                message = "Password is required."
        )
        @Size(
                min = 8,
                max = 72,
                message = "Password must contain between 8 and 72 characters."
        )
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
                message = "Password must contain uppercase, lowercase, number, and special characters, with no spaces."
        )
        String password,


        @NotNull(
                message = "Department ID is required."
        )
        @Positive(
                message = "Department ID must be greater than zero."
        )
        Long departmentId

) {
}