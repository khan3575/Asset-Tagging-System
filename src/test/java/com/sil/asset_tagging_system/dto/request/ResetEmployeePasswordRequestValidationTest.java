package com.sil.asset_tagging_system.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ResetEmployeePasswordRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid password should produce zero violations")
    void validPassword_HasNoViolations() {
        ResetEmployeePasswordRequest request = new ResetEmployeePasswordRequest("NewSecret123!");

        Set<ConstraintViolation<ResetEmployeePasswordRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Blank password should trigger @NotBlank violation")
    void blankPassword_TriggersViolation() {
        ResetEmployeePasswordRequest request = new ResetEmployeePasswordRequest("");

        Set<ConstraintViolation<ResetEmployeePasswordRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("New password is required.");
    }

    @Test
    @DisplayName("Short password under 8 characters should trigger @Size violation")
    void shortPassword_TriggersViolation() {
        ResetEmployeePasswordRequest request = new ResetEmployeePasswordRequest("Short1!");

        Set<ConstraintViolation<ResetEmployeePasswordRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("New password must be between 8 and 72 characters.");
    }

    @Test
    @DisplayName("Weak password missing special characters should trigger @Pattern violation")
    void weakPassword_TriggersViolation() {
        ResetEmployeePasswordRequest request = new ResetEmployeePasswordRequest("lowercase1234");

        Set<ConstraintViolation<ResetEmployeePasswordRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("New password must contain uppercase, lowercase, number, and special characters, with no spaces.");
    }
}
