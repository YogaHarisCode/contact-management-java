package contactmanagement.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request body of {@code POST /api/users} used to register a new user.
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
public class RegisterUserRequest {

    /** Desired unique username. Required, not blank, maximum 100 characters. */
    @NotBlank
    @Size(max = 100)
    private String username;

    /** Plain-text password, hashed before storage. Required, 8 to 100 characters. */
    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    /** Display name of the user. Required, not blank, maximum 100 characters. */
    @NotBlank
    @Size(max = 100)
    private String name;
}