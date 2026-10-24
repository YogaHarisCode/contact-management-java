package contactmanagement.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Request body of {@code POST /api/auth/login} used to authenticate a user.
 *
 * <p>Validated with Bean Validation when the controller parameter is annotated with
 * {@code @Valid}. The generated {@code toString()} excludes the password so the payload can
 * never leak a plain-text password through logging.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "password")
public class LoginUserRequest {

    /** Username of the account to log in to. Required, not blank, maximum 100 characters. */
    @NotBlank
    @Size(max = 100)
    private String username;

    /** Plain-text password to verify against the stored hash. Required, not blank, maximum 100 characters. */
    @NotBlank
    @Size(max = 100)
    private String password;
}