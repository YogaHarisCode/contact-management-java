package contactmanagement.service;

import contactmanagement.model.LoginResponse;
import contactmanagement.model.LoginUserRequest;

/**
 * Business operations related to authentication.
 */
public interface AuthService {

    /**
     * Authenticates a user and issues a new session token, replacing any previous token.
     *
     * @param request the validated login credentials
     * @return the issued token and its expiration time (epoch milliseconds)
     * @throws org.springframework.web.server.ResponseStatusException with status 401 when the
     *         username does not exist or the password does not match
     */
    LoginResponse login(LoginUserRequest request);
}