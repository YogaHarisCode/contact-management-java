package com.yogaharis.contactmanagement.constraint;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

/**
 * Validator behind {@link MaxBytes}. Checks that a string does not exceed the
 * configured size when encoded as UTF-8.
 *
 * @author Yoga Harisistianugroho
 */
public class MaxBytesValidator implements ConstraintValidator<MaxBytes, String> {

    /** Maximum allowed size in UTF-8 bytes, read from {@link MaxBytes#value()}. */
    private int maxBytes;

    /**
     * Reads the byte limit from the annotation.
     *
     * @param constraintAnnotation the {@link MaxBytes} annotation instance on the validated field
     */
    @Override
    public void initialize(MaxBytes constraintAnnotation) {
        maxBytes = constraintAnnotation.value();
    }

    /**
     * Checks the size of the value in UTF-8 bytes.
     *
     * @param value   the string to validate
     * @param context the validation context (not used)
     * @return {@code true} if the value is {@code null} or within the byte limit, {@code false} otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null is valid here; presence is the job of @NotBlank.
        if (value == null){
            return true;
        }

        // Count bytes, not characters: BCrypt's 72-byte limit applies to the encoded bytes.
        int valueBytes = value.getBytes(StandardCharsets.UTF_8).length;

        return valueBytes <= maxBytes;
    }
}