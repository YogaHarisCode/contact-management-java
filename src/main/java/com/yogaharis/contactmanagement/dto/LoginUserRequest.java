package com.yogaharis.contactmanagement.dto;

import com.yogaharis.contactmanagement.constraint.MaxBytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request body of {@code POST /api/auth/login}.
 *
 * <p>The generated {@code toString()} excludes the password so it can never end up in logs.</p>
 *
 * @author Yoga Harisistianugroho
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"password"})
public class LoginUserRequest {

    /** Username of the account to log in to. Must not be blank; max 100 characters. */
    @NotBlank
    @Size(max = 100)
    private String username;

    /**
     * Raw password of the account. Must not be blank; max 72 bytes in UTF-8
     * (see {@link MaxBytes}), which is the most BCrypt reads.
     */
    @MaxBytes(72)
    @NotBlank
    private String password;
}