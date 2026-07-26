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

public class UpdateEmployeeRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid update request should produce zero violations")
    void validRequest_HasNoViolations() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "Jane", "Doe", "jane.doe@test.com", 2L
        );

        Set<ConstraintViolation<UpdateEmployeeRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Blank name should trigger @NotBlank violation")
    void blankName_TriggersViolation() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "", "Doe", "jane.doe@test.com", 2L
        );

        Set<ConstraintViolation<UpdateEmployeeRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("First name is required.");
    }

    @Test
    @DisplayName("Invalid email should trigger @Email violation")
    void invalidEmail_TriggersViolation() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "Jane", "Doe", "not-an-email", 2L
        );

        Set<ConstraintViolation<UpdateEmployeeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Enter a valid email address.");
    }

    @Test
    @DisplayName("Null department ID should trigger @NotNull violation")
    void nullDepartmentId_TriggersViolation() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "Jane", "Doe", "jane.doe@test.com", null
        );

        Set<ConstraintViolation<UpdateEmployeeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Department ID is required.");
    }
}
