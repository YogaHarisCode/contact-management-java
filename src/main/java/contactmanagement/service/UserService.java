package contactmanagement.service;

import contactmanagement.entity.User;
import contactmanagement.model.RegisterUserRequest;
import contactmanagement.model.UpdateUserRequest;
import contactmanagement.model.UserResponse;

/**
 * Business operations related to application users.
 */
public interface UserService {

    /**
     * Registers a new user.
     *
     * <p>The password is hashed with BCrypt before it is stored.
     *
     * @param request the validated registration data
     * @return the created user without any sensitive field
     * @throws org.springframework.web.server.ResponseStatusException with status 400 when the
     *         username is already registered
     */
    UserResponse register(RegisterUserRequest request);

    /**
     * Returns the public profile of an already authenticated user.
     *
     * @param user the authenticated user, resolved from the {@code X-API-TOKEN} header
     * @return the user's username and name, without the password hash or token
     */
    UserResponse get(User user);

    UserResponse update(User user, UpdateUserRequest request);
}