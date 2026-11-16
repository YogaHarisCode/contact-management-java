package com.yogaharis.contactmanagement.constraint;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {MaxBytesValidator.class})
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxBytes {

    int value();

    String message() default "must not exceed {value} bytes";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
