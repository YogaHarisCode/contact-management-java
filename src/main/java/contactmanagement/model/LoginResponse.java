package contactmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data returned after a successful login.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {

    /** Opaque authentication token to send in the {@code X-API-TOKEN} header. */
    private String token;

    /** Expiration time of the token in epoch milliseconds. */
    private Long tokenExpiredAt;
}