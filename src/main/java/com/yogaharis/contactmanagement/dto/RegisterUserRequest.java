package com.yogaharis.contactmanagement.dto;

import com.yogaharis.contactmanagement.constraint.MaxBytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request body for registering a new user ({@code POST /api/users}).
 *
 * <p>The password is excluded from {@link #toString()} so it can never leak into logs.</p>
 *
 * @author Yoga Harisistianugroho
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"password"})
public class RegisterUserRequest {

    /**
     * Desired login name. Required, at most 100 characters, and limited to
     * letters, digits, dot, underscore and hyphen (no whitespace).
     */
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "must not contain whitespace")
    @Size(max = 100)
    private String username;

    /**
     * Raw password. Required and at most 72 bytes in UTF-8, which is the BCrypt
     * input limit. It is hashed before storage and never returned in a response.
     */
    @NotBlank
    @MaxBytes(72)
    @Size(min = 8)
    private String password;

    /** Display name of the user. Required and at most 100 characters. */
    @NotBlank
    @Size(max = 100)
    private String name;
}