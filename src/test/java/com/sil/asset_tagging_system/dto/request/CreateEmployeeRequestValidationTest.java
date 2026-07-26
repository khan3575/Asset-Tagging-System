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

public class CreateEmployeeRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid payload should produce zero constraint violations")
    void validRequest_HasNoValidationViolations() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Mostafiz",
                "Fahim",
                "mostafiz.fahim@test.com",
                "Password123!",
                1L
        );

        Set<ConstraintViolation<CreateEmployeeRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Blank first name should trigger @NotBlank violation")
    void blankFirstName_TriggersValidationViolation() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "",
                "Fahim",
                "mostafiz.fahim@test.com",
                "Password123!",
                1L
        );

        Set<ConstraintViolation<CreateEmployeeRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("First name is required.");
    }

    @Test
    @DisplayName("First name with invalid characters should trigger @Pattern violation")
    void invalidNamePattern_TriggersValidationViolation() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "John123",
                "Doe",
                "john@test.com",
                "Password123!",
                1L
        );

        Set<ConstraintViolation<CreateEmployeeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("First name contains invalid characters.");
    }

    @Test
    @DisplayName("Invalid email format should trigger @Email violation")
    void invalidEmail_TriggersValidationViolation() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "John",
                "Doe",
                "invalid-email-format",
                "Password123!",
                1L
        );

        Set<ConstraintViolation<CreateEmployeeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Enter a valid email address.");
    }

    @Test
    @DisplayName("Password shorter than 8 characters should trigger @Size violation")
    void shortPassword_TriggersValidationViolation() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "John",
                "Doe",
                "john@test.com",
                "Pass1!",
                1L
        );

        Set<ConstraintViolation<CreateEmployeeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Password must be between 8 and 72 characters.");
    }

    @Test
    @DisplayName("Password missing special character/uppercase should trigger @Pattern violation")
    void weakPasswordPattern_TriggersValidationViolation() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "John",
                "Doe",
                "john@test.com",
                "lowercaseonly123",
                1L
        );

        Set<ConstraintViolation<CreateEmployeeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Password must contain uppercase, lowercase, number, and special characters, with no spaces.");
    }

    @Test
    @DisplayName("Null department ID should trigger @NotNull violation")
    void nullDepartmentId_TriggersValidationViolation() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "John",
                "Doe",
                "john@test.com",
                "Password123!",
                null
        );

        Set<ConstraintViolation<CreateEmployeeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Department ID is required.");
    }

    @Test
    @DisplayName("Negative department ID should trigger @Positive violation")
    void negativeDepartmentId_TriggersValidationViolation() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "John",
                "Doe",
                "john@test.com",
                "Password123!",
                -5L
        );

        Set<ConstraintViolation<CreateEmployeeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Department ID must be greater than zero.");
    }
}
