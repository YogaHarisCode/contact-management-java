package com.yogaharis.contactmanagement.service;

import com.yogaharis.contactmanagement.dto.LoginUserRequest;
import com.yogaharis.contactmanagement.dto.TokenResponse;
import com.yogaharis.contactmanagement.entity.User;
import com.yogaharis.contactmanagement.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles user authentication: checks credentials and issues an API token.
 *
 * <p>A token is a random UUID valid for 30 days. It is stored on the user's row, so a user
 * has at most one active token at a time.</p>
 *
 * @author Yoga Harisistianugroho
 */
@Service
@Slf4j
public class AuthService {

    private static final String USERNAME_OR_PASSWORD_WRONG = "Username or password wrong";
    private static final String UNAUTHORIZED = "Unauthorized";
    // Password hashed once at startup to build dummyPasswordHash; its value is irrelevant.
    private static final String DUMMY_PASSWORD = "timing-equalizer";
    private static final Duration TOKEN_VALIDITY = Duration.ofDays(30);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final Clock clock;

    /**
     * Hash checked when the username does not exist, so a missing user costs the same
     * BCrypt time as a wrong password. Built with the real encoder to match its cost factor.
     */
    private final String dummyPasswordHash;

    /**
     * Creates the service and prepares the dummy hash used to equalize login timing.
     *
     * <p>Hashing the dummy password costs one BCrypt operation, done once at startup.</p>
     *
     * @param userRepository  repository used to load and save users
     * @param passwordEncoder encoder used to hash the dummy password and verify login passwords
     * @param clock           clock used to compute the token expiry time
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(DUMMY_PASSWORD);
    }

    /**
     * Authenticates a user and issues a new API token.
     *
     * <p>Exactly one password check runs whether or not the username exists (against a dummy
     * hash when it does not), so response time does not reveal which usernames are registered.
     * A successful login replaces any token the user already had.</p>
     *
     * @param request the validated login request with the username and raw password
     * @return the new token and its expiry time in epoch milliseconds
     * @throws ResponseStatusException with status {@code 401 UNAUTHORIZED} when the username
     *                                 does not exist or the password is wrong
     */
    @Transactional
    public TokenResponse login(LoginUserRequest request) {
        // Only the length is logged: the raw value may be a mistyped password.
        log.debug("Login attempt received: usernameLength={}", request.getUsername().length());

        Optional<User> found = userRepository.findById(request.getUsername());

        // Always run exactly one BCrypt check, whether or not the user exists.
        String storedHash = found.map(User::getPassword).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), storedHash);

        if (found.isEmpty()) {
            // The username does not exist, so it may be a mistyped password: log only its length.
            log.warn("Login user rejected: reason=user_not_found, usernameLength={}", request.getUsername().length());
            throw invalidCredentials(USERNAME_OR_PASSWORD_WRONG);
        }
        if (!passwordMatches) {
            log.warn("Login user rejected: reason=password_mismatch, username={}", request.getUsername());
            throw invalidCredentials(USERNAME_OR_PASSWORD_WRONG);
        }

        // A fresh token on every login: it replaces the previous one, which stops working immediately.
        User user = found.get();
        user.setToken(UUID.randomUUID().toString());
        user.setTokenExpiredAt(clock.instant().plus(TOKEN_VALIDITY).toEpochMilli());
        userRepository.save(user);

        log.info("Login successful: username={}", request.getUsername());
        return TokenResponse.builder()
                .token(user.getToken())
                .tokenExpiredAt(user.getTokenExpiredAt())
                .build();
    }

    @Transactional
    public void logout(User user) {
        String username = user.getUsername();
        log.debug("Logout attempt received: username={}", username);

        User managedUser = userRepository.findById(username)
                .orElseThrow(() -> {
                    log.warn("Logout rejected: reason=user_not_found, username={}", username);
                    return invalidCredentials(UNAUTHORIZED);
                });

        if (!Objects.equals(user.getToken(), managedUser.getToken())) {
            log.warn("Logout rejected: reason=token_replaced, username={}", username);
            throw invalidCredentials(UNAUTHORIZED);
        }

        managedUser.setToken(null);
        managedUser.setTokenExpiredAt(null);
        userRepository.save(managedUser);
        log.info("Logout successful: username={}", username);
    }

    /**
     * Always returns the same generic 401 for a given message, so clients cannot tell a missing user from a wrong password.
     *
     * @param message the message returned to the client
     * @return a {@code 401 UNAUTHORIZED} exception with the given message
     */
    private ResponseStatusException invalidCredentials(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}