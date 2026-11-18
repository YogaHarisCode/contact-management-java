package com.yogaharis.contactmanagement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Error payload placed in {@link WebResponse#getErrors()} when a request fails.
 *
 * @author Yoga Harisistianugroho
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Error code. Either an HTTP status such as {@code 409 CONFLICT}, or
     * {@code VALIDATION_ERROR} for request validation failures.
     */
    private String code;

    /** Human-readable description of what went wrong. */
    private String message;

    /** Per-field problems. Only present for validation errors; omitted otherwise. */
    private List<ErrorDetail> details;
}