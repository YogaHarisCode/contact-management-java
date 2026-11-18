package com.yogaharis.contactmanagement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard envelope for every API response.
 *
 * <p>A successful response carries {@code data}; a failed one carries {@code errors}.
 * The field that does not apply is omitted from the JSON output.</p>
 *
 * @param <T> the type of the response payload
 * @author Yoga Harisistianugroho
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebResponse <T>{

    /** Response payload. {@code null} (and omitted) when the request failed. */
    private T data;

    /** Error information. {@code null} (and omitted) when the request succeeded. */
    private ErrorResponse errors;
}