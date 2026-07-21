package com.sil.asset_tagging_system.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


public class LoginRequestValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setup()
    {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void blankEmailValidationTest() throws Exception{
        LoginRequest request = new LoginRequest("", "password123");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void blankPasswordValidationTest() throws Exception{
        LoginRequest request = new LoginRequest("sakib@gmail.com", "");
        Set<ConstraintViolation<LoginRequest>> violations= validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void allSpacePasswordValidationTest() throws Exception{
        LoginRequest request = new LoginRequest("sakib@gmail.com", "         ");
        Set<ConstraintViolation<LoginRequest>> violations= validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void lessThanEightCharacterValidationTest() throws Exception{

        LoginRequest request = new LoginRequest("sakib@gmail.com", "Ab1@");
        Set<ConstraintViolation<LoginRequest>> violations= validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void moreThanTwelveCharacterValidationTest() throws Exception{

        LoginRequest request = new LoginRequest("sakib@gmail.com", "Ab1@.asdfasfdas");
        Set<ConstraintViolation<LoginRequest>> violations= validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void noUpperCaseCharacterValidationTest() throws Exception{

        LoginRequest request = new LoginRequest("sakib@gmail.com", "b1@bbbbb");
        Set<ConstraintViolation<LoginRequest>> violations= validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void noLowerCaseCharacterValidationTest() throws Exception{

        LoginRequest request = new LoginRequest("sakib@gmail.com", "BB1@BBBBB");
        Set<ConstraintViolation<LoginRequest>> violations= validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void noDigitCharacterValidationTest() throws Exception{
        LoginRequest request = new LoginRequest("sakib@gmail.com", "Abaaaa@a");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void noSpecialCharacterValidationTest() throws Exception{
        LoginRequest request = new LoginRequest("sakib@gmail.com", "Ab123aaaa");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void isEmailValidationTest() throws Exception{
        LoginRequest request = new LoginRequest("sakib#gmail.com", "Ab123aaa@");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }
    @Test
    void emailWithoutDomainValidationTest() throws Exception{
        LoginRequest request = new LoginRequest("sakib@.com", "Ab123aaa@");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void correctEmailAndPasswordValidationTest() throws Exception{

        LoginRequest request = new LoginRequest("sakib@gmail.com", "Ab123aaa@");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

}
