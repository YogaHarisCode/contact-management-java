package com.yogaharis.contactmanagement.service;

import com.yogaharis.contactmanagement.dto.RegisterUserRequest;
import com.yogaharis.contactmanagement.dto.UpdateUserRequest;
import com.yogaharis.contactmanagement.dto.UserResponse;
import com.yogaharis.contactmanagement.entity.User;
import com.yogaharis.contactmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * Business logic for user management.
 *
 * @author Yoga Harisistianugroho
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    /** Hashes passwords before they are stored. */
    private final PasswordEncoder passwordEncoder;

    /** Persistence access for {@link User} entities. */
    private final UserRepository userRepository;

    /** Error message returned when a username is already taken. */
    private static final String USERNAME_ALREADY_EXISTS = "Username already exists";

    /** Error message returned when the authenticated user no longer exists. */
    private static final String UNAUTHORIZED_MESSAGE = "Unauthorized";

    /**
     * Registers a new user.
     *
     * <p>The password is hashed with BCrypt before the user is saved.</p>
     *
     * @param request the validated registration data (username, raw password, name)
     * @return the created user's public data (username and name, never the password)
     * @throws ResponseStatusException with status {@code 409 CONFLICT} if the username is already taken
     */
    @Transactional
    public UserResponse register(RegisterUserRequest request){
        // Fail fast: reject a taken username before paying for the (deliberately slow) BCrypt hash.
        boolean exists = userRepository.existsById(request.getUsername());

        if (exists){
            log.warn("Register user rejected: reason={}, username={}", USERNAME_ALREADY_EXISTS,request.getUsername());
            throw responseStatusException(HttpStatus.CONFLICT, USERNAME_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        // Only the BCrypt hash is stored; the raw password never reaches the database.
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());


        try {
            // saveAndFlush (not save) sends the INSERT inside this try block. If a concurrent request
            // registered the same username after the check above, the database's primary key
            // rejects it here and it is mapped to 409 instead of surfacing as a 500 at commit time.
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e){
            log.warn("Register user rejected: reason={}, username={}", USERNAME_ALREADY_EXISTS,request.getUsername());
            throw responseStatusException(HttpStatus.CONFLICT, USERNAME_ALREADY_EXISTS);
        }

        log.info("User registered: username={}", user.getUsername());

        return toUserResponse(user);
    }

    /**
     * Returns the public data of the authenticated user.
     *
     * <p>The user is already authenticated and loaded by {@code UserArgumentResolver}, so this
     * method only maps it to a response and does not access the database.</p>
     *
     * @param user the authenticated user
     * @return the user's public data (username and name, never the password or token)
     */
    public UserResponse get(User user){
        log.debug("Get current user: username={}", user.getUsername());

        return toUserResponse(user);
    }

    /**
     * Updates the name and/or password of the authenticated user.
     *
     * <p>Only the fields that are not {@code null} in the request are changed. A new password is hashed
     * with BCrypt before it is stored. If both fields are {@code null}, nothing is changed and the
     * database is not accessed. The token and its expiry are never modified.</p>
     *
     * <p>The given user was loaded by {@code UserArgumentResolver} outside this transaction, so it can be
     * stale (for example, a login may have issued a newer token since). The user is therefore reloaded
     * by username and the changes are applied to that fresh entity.</p>
     *
     * @param user    the authenticated user, supplied by {@code UserArgumentResolver}; its username is used
     *                to reload the user, and it is returned as-is when there is nothing to update
     * @param request the validated update data; {@code null} fields are left unchanged
     * @return the user's public data after the update (username and name, never the password or token)
     * @throws ResponseStatusException with status {@code 401 UNAUTHORIZED} if the user no longer exists
     */
    @Transactional
    public UserResponse update(User user, UpdateUserRequest request){
        log.debug("Update user requested: username={}, nameProvided={}, passwordProvided={}",
                user.getUsername(), Objects.nonNull(request.getName()), Objects.nonNull(request.getPassword()));

        // Nothing to change: answer from the given user without touching the database.
        if (Objects.isNull(request.getName()) && Objects.isNull(request.getPassword())){
            log.debug("Update user skipped: reason=no_fields_provided, username={}", user.getUsername());
            return toUserResponse(user);
        }

        // The given user may be stale. Reloading it means only the changed columns are written and a
        // token issued by a concurrent login is not overwritten with old data.
        User managedUser = userRepository.findById(user.getUsername())
                .orElseThrow(() -> {
                    log.warn("Update user rejected: reason=user_not_found, username={}", user.getUsername());
                    return responseStatusException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
                });

        // A null field means "not provided", so the stored value is kept.
        if (Objects.nonNull(request.getName())){
            managedUser.setName(request.getName());
        }

        if (Objects.nonNull(request.getPassword())){
            // Only the BCrypt hash is stored; the raw password never reaches the database.
            managedUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User savedUser = userRepository.save(managedUser);

        log.info("User updated: username={}, nameUpdated={}, passwordUpdated={}",
                savedUser.getUsername(), Objects.nonNull(request.getName()), Objects.nonNull(request.getPassword()));

        return toUserResponse(savedUser);
    }

    /**
     * Builds a {@link ResponseStatusException} with the given status and message.
     *
     * @param statusCode the HTTP status to respond with
     * @param message    the reason returned to the client
     * @return the exception, ready to be thrown
     */
    private ResponseStatusException responseStatusException(HttpStatusCode statusCode, String message){
        return new ResponseStatusException(statusCode, message);
    }

    /**
     * Maps a {@link User} entity to its public response form.
     *
     * @param user the entity to map
     * @return a {@link UserResponse} without the password
     */
    private UserResponse toUserResponse(User user){
        return UserResponse.builder()
                .username(user.getUsername()).name(user.getName()).build();
    }
}