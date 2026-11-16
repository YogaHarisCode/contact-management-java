package com.yogaharis.contactmanagement.constraint;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class MaxBytesValidator implements ConstraintValidator<MaxBytes, String> {

    private int maxBytes;

    @Override
    public void initialize(MaxBytes constraintAnnotation) {
        maxBytes = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null){
            return true;
        }

        int valueBytes = value.getBytes(StandardCharsets.UTF_8).length;

        return valueBytes <= maxBytes;
    }
}
