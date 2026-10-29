package contactmanagement.service;

import contactmanagement.entity.User;
import contactmanagement.model.LoginResponse;
import contactmanagement.model.LoginUserRequest;
import contactmanagement.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Default {@link AuthService} implementation backed by Spring Data JPA.
 *
 * <p>Passwords are verified against BCrypt hashes through the configured
 * {@link PasswordEncoder}. A session token is a random UUID stored on the {@link User} row,
 * together with its expiration time.
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    /** Error message returned for every failed login, regardless of the cause. */
    private static final String INVALID_CREDENTIALS = "Username or password wrong";

    /** Lifetime of an issued token. */
    private static final Duration TOKEN_TTL = Duration.ofDays(30);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * BCrypt hash of a random value, compared against when the username does not exist so that
     * unknown and known usernames take a similar amount of time to reject.
     */
    private final String dummyPasswordHash;

    /**
     * Creates the service and prepares the dummy hash used for timing-safe rejection.
     *
     * <p>The dummy hash is created once here, with the same encoder (and therefore the same
     * BCrypt cost) as real user hashes.
     *
     * @param userRepository  repository used to load and update users
     * @param passwordEncoder encoder used to verify passwords and to build the dummy hash
     */
    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * {@inheritDoc}
     *
     * <p>The token and expiry are set on the managed {@link User} entity; they are written to
     * the database by JPA dirty checking when the transaction commits.
     */
    @Override
    @Transactional
    public LoginResponse login(LoginUserRequest request) {
        String username = request.getUsername();
        log.debug("Login started: username={}", username);

        User user = userRepository.findById(username).orElse(null);

        // BCrypt always runs, even for an unknown username, so response time does not reveal
        // which usernames exist.
        String hashToCheck = (user != null) ? user.getPassword() : dummyPasswordHash;
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), hashToCheck);

        if (user == null || !passwordMatches) {
            // The reason is logged for operators only; the client always gets the same message.
            log.warn("Login rejected, invalid credentials: username={}, reason={}",
                    username, (user == null) ? "user_not_found" : "wrong_password");
            throw invalidCredentials();
        }

        // A new login replaces any earlier token, so a user has one active token at a time.
        user.setToken(UUID.randomUUID().toString());
        user.setTokenExpiredAt(Instant.now().plus(TOKEN_TTL).toEpochMilli());

        // The token itself is deliberately never logged.
        log.info("User logged in: username={}, tokenExpiredAt={}", username, user.getTokenExpiredAt());

        return LoginResponse.builder()
                .token(user.getToken())
                .tokenExpiredAt(user.getTokenExpiredAt())
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The change is applied to the user that was loaded while resolving the token, so no
     * extra lookup is needed. Only the username is logged, never the token.
     */
    @Override
    @Transactional
    public void logout(User user) {
        String username = user.getUsername();
        log.debug("Logout started: username={}", username);

        // With both columns null, UserRepository#findFirstByToken no longer matches the old token.
        user.setToken(null);
        user.setTokenExpiredAt(null);

        // The loaded user is not "new" (see User#isNew), so save() merges instead of inserting.
        userRepository.save(user);

        log.info("User logged out: username={}", username);
    }

    /**
     * Builds the exception used for every failed login.
     *
     * @return a 401 exception carrying the standard error message
     */
    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
    }
}