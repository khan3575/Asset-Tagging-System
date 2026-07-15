package com.sil.asset_tagging_system.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LoginRequest {
    @Email
    @NotBlank(message="Email can not be blank")
    private String email;

    @NotBlank(message="Password can not be blank")
    @Size(min=8, max=12)
    @Pattern(
            regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
            message="Password must contain at least 1 lowercase, 1 uppercase, 1 digit, 1 special character"
    )
    private String password;

    @Override
    public String toString() {
        return "LoginRequest{" +
                "email='" + email + '\'' +
                "password is empty = {}"+ (password==null || password.isEmpty())+
                '}';
    }
}
