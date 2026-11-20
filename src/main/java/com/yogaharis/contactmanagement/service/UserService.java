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

    @Transactional
    public UserResponse update(User user, UpdateUserRequest request){
        if (Objects.isNull(request.getName()) && Objects.isNull(request.getPassword())){
            return toUserResponse(user);
        }

        if (Objects.nonNull(request.getName())){
            user.setName(request.getName());
        }

        if (Objects.nonNull(request.getPassword())){
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return toUserResponse(userRepository.save(user));
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