package com.yogaharis.contactmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single field-level validation problem, listed in {@link ErrorResponse#getDetails()}.
 *
 * @author Yoga Harisistianugroho
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorDetail {

    /** Name of the request field that failed validation, e.g. {@code username}. */
    private String field;

    /** Human-readable description of the violated constraint, e.g. {@code must not be blank}. */
    private String message;
}