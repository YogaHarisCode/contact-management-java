package com.yogaharis.contactmanagement.constraint;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation constraint that limits a {@link String} to a maximum size in
 * <strong>UTF-8 bytes</strong> rather than characters.
 *
 * <p>This matters for BCrypt, which only reads the first 72 bytes of a password:
 * a multi-byte character such as an emoji takes up to 4 bytes, so a short string
 * can still exceed the limit. {@code null} values are considered valid; combine
 * with {@code @NotBlank} to reject missing values.</p>
 *
 * @author Yoga Harisistianugroho
 * @see MaxBytesValidator
 */
@Documented
@Constraint(validatedBy = {MaxBytesValidator.class})
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxBytes {

    /**
     * Maximum allowed size in UTF-8 bytes.
     *
     * @return the byte limit
     */
    int value();

    /**
     * Message used when validation fails. {@code {value}} is replaced with the byte limit.
     *
     * @return the violation message
     */
    String message() default "must not exceed {value} bytes";

    /**
     * Validation groups this constraint belongs to (standard Bean Validation attribute).
     *
     * @return the groups
     */
    Class<?>[] groups() default { };

    /**
     * Payload for clients of the constraint (standard Bean Validation attribute).
     *
     * @return the payload types
     */
    Class<? extends Payload>[] payload() default { };
}