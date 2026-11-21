package com.yogaharis.contactmanagement.dto;

import com.yogaharis.contactmanagement.constraint.MaxBytes;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request body for updating the authenticated user ({@code PATCH /api/users/current}).
 *
 * <p>Both fields are optional. A field that is {@code null} (left out of the JSON, or sent as
 * {@code null}) is treated as "not provided" and the stored value is left unchanged. A field that is
 * provided must pass its validation rules.</p>
 *
 * @author Yoga Harisistianugroho
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "password")
public class UpdateUserRequest {

    /**
     * The new display name. Optional: a {@code null} value skips validation and keeps the current name.
     * When provided it must not be blank (at least one non-whitespace character) and must be at most
     * 100 characters.
     */
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    @Size(max = 100)
    private String name;

    /**
     * The new raw password. Optional: a {@code null} value skips validation and keeps the current
     * password. When provided it must not be blank, must be at least 8 characters, and must be at most
     * 72 bytes in UTF-8 (BCrypt only reads the first 72 bytes). It is excluded from {@code toString()}.
     */
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    @Size(min = 8)
    @MaxBytes(72)
    private String password;
}