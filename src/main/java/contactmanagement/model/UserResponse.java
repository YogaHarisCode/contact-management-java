package contactmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public representation of a user returned by the API.
 *
 * <p>Deliberately contains no password, hash, or token, so sensitive data cannot leak even if
 * a mapping is extended later.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {

    /** Unique username of the user. */
    private String username;

    /** Display name of the user. */
    private String name;
}