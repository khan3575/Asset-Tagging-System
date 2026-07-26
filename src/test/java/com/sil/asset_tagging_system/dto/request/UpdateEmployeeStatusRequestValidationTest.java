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

public class UpdateEmployeeStatusRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid status request should produce zero violations")
    void validStatusRequest_HasNoViolations() {
        UpdateEmployeeStatusRequest request = new UpdateEmployeeStatusRequest(true);

        Set<ConstraintViolation<UpdateEmployeeStatusRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Null enabled flag should trigger @NotNull violation")
    void nullEnabledFlag_TriggersViolation() {
        UpdateEmployeeStatusRequest request = new UpdateEmployeeStatusRequest(null);

        Set<ConstraintViolation<UpdateEmployeeStatusRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Employee account status is required.");
    }
}
