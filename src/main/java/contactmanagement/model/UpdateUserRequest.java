package contactmanagement.model;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Request body of {@code PATCH /api/users/current} used to update the authenticated user.
 *
 * <p>Both fields are optional: a field that is {@code null} (omitted from the JSON) keeps its
 * current value. A field that is present must satisfy its constraints, so an empty or
 * whitespace-only value is rejected instead of being treated as "not provided".
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
public class UpdateUserRequest {

    /**
     * New display name. Optional; when present it must be at most 100 characters and contain
     * at least one non-whitespace character.
     */
    // @Pattern treats null as valid, which keeps the field optional; "(?s)" lets \S match across line breaks.
    @Size(max = 100)
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    private String name;

    /**
     * New plain-text password, hashed with BCrypt before storage. Optional; when present it
     * must be 8 to 100 characters and contain at least one non-whitespace character.
     */
    @Size(min = 8, max = 100)
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    private String password;
}