package contactmanagement.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Request body of {@code POST /api/auth/login}.
 *
 * <p>Unlike registration, the password has no minimum length here: the password policy is
 * enforced when a password is created, and a wrong password must always result in 401, not 400.
 * The generated {@code toString()} excludes the password so it cannot leak through logging.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "password")
public class LoginUserRequest {

    /** Username of the account. Required, not blank, maximum 100 characters. */
    @NotBlank
    @Size(max = 100)
    private String username;

    /** Plain-text password to verify. Required, not blank, maximum 100 characters. */
    @NotBlank
    @Size(max = 100)
    private String password;
}