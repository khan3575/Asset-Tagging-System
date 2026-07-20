package com.sil.asset_tagging_system.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record LoginRequest (

        @Email
        @NotBlank(message="Email can not be blank")
        String email,

        @NotBlank(message="Password can not be blank")
        @Size(min=8, max=12)
        @Pattern(
            regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
            message="Password must contain at least 1 lowercase, 1 uppercase, 1 digit, 1 special character"
        )
        String password
    ){}
 