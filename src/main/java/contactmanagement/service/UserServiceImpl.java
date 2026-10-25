package contactmanagement.service;

import contactmanagement.entity.User;
import contactmanagement.model.RegisterUserRequest;
import contactmanagement.model.UserResponse;
import contactmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Default {@link UserService} implementation backed by Spring Data JPA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String USERNAME_ALREADY_EXISTS = "Username already exists";

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     * {@inheritDoc}
     *
     * <p>The username is checked up front to return a clear error in the common case. The
     * database primary key remains the authoritative guard against concurrent registrations
     * of the same username.
     */
    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        String username = request.getUsername();
        log.debug("Register user started: username={}", username);

        // Fast path: reject a known duplicate before doing the expensive BCrypt hashing.
        if (userRepository.existsById(username)) {
            log.warn("Register user rejected, username already exists: username={}", username);
            throw usernameAlreadyExists();
        }

        User user = new User();
        user.setUsername(username);
        // Never store the raw password: only the BCrypt hash is persisted.
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());

        try {
            // saveAndFlush forces the INSERT now, so a duplicate-key violation surfaces in this try block.
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Another request registered the same username between the check above and the insert.
            log.warn("Register user rejected, duplicate detected by database constraint "
                    + "(concurrent registration): username={}", username);
            throw usernameAlreadyExists();
        }

        log.info("User registered: username={}", username);
        return toUserResponse(user);
    }

    @Override
    public UserResponse get(User user) {
        return toUserResponse(user);
    }

    /**
     * Builds the exception used when a username is already taken.
     *
     * @return a 400 exception carrying the standard error message
     */
    private ResponseStatusException usernameAlreadyExists() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, USERNAME_ALREADY_EXISTS);
    }

    /**
     * Maps an entity to its public representation, leaving out the password hash and token.
     *
     * @param user the persisted user
     * @return the response DTO
     */
    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .username(user.getUsername())
                .name(user.getName())
                .build();
    }
}