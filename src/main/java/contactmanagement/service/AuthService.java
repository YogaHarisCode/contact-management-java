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

    void logout(User user);
}