package contactmanagement.service;

import contactmanagement.entity.User;
import contactmanagement.model.LoginResponse;
import contactmanagement.model.LoginUserRequest;

/**
 * Business operations related to user authentication.
 */
public interface AuthService {

    /**
     * Authenticates a user and issues a new session token.
     *
     * <p>Any previously issued token of the same user is replaced.
     *
     * @param request the validated login credentials
     * @return the new token and its expiration time
     * @throws org.springframework.web.server.ResponseStatusException with status 401 when the
     *         username does not exist or the password does not match; both cases use the same
     *         message so the response does not reveal which usernames are registered
     */
    LoginResponse login(LoginUserRequest request);

    /**
     * Ends the session of an authenticated user by invalidating the current token.
     *
     * <p>The token and its expiration time are cleared, so the token can no longer authenticate
     * requests. The username, password hash, and name are not modified. Calling this method for
     * a user that has no token is harmless.
     *
     * @param user the authenticated user, resolved from the {@code X-API-TOKEN} header
     */
    void logout(User user);
}