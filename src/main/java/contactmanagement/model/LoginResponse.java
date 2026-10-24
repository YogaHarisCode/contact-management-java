package contactmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload returned by a successful login, carried in the {@code data} field of
 * {@link WebResponse}.
 *
 * <p>Contains only the authentication token and its expiry; it never includes the password
 * or its hash.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {

    /** Randomly generated (UUID) token that identifies the authenticated session. */
    private String token;

    /** Expiration time of {@link #token}, as epoch milliseconds (UTC). */
    private Long tokenExpiredAt;
}