package com.yogaharis.contactmanagement.controller;

import com.yogaharis.contactmanagement.dto.RegisterUserRequest;
import com.yogaharis.contactmanagement.dto.UpdateUserRequest;
import com.yogaharis.contactmanagement.dto.UserResponse;
import com.yogaharis.contactmanagement.dto.WebResponse;
import com.yogaharis.contactmanagement.entity.User;
import com.yogaharis.contactmanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user endpoints, mounted at {@code /api/users}.
 *
 * <p>It only handles HTTP concerns (request binding, validation trigger, response
 * wrapping); business rules live in {@link UserService}.</p>
 *
 * @author Yoga Harisistianugroho
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/users")
public class UserController {

    /** Service that implements the user use cases. */
    private final UserService userService;

    /**
     * Registers a new user ({@code POST /api/users}).
     *
     * @param request the registration data; validated with {@code @Valid} before this method runs
     * @return a {@link WebResponse} whose {@code data} holds the created user's username and name
     */
    @PostMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<UserResponse> register(@Valid @RequestBody RegisterUserRequest request){
        UserResponse response = userService.register(request);
        return WebResponse.<UserResponse>builder().data(response).build();
    }

    /**
     * Returns the profile of the authenticated user ({@code GET /api/users/current}).
     *
     * <p>The {@code user} parameter is not read from the request body. It is resolved from the
     * {@code X-API-TOKEN} header by {@code UserArgumentResolver}, which rejects the request with
     * {@code 401 UNAUTHORIZED} before this method runs if the header is missing or blank, the token
     * is unknown, or the token has expired.</p>
     *
     * @param user the authenticated user, supplied by {@code UserArgumentResolver}
     * @return a {@link WebResponse} whose {@code data} holds the user's username and name
     */
    @GetMapping(
            path = "/current",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<UserResponse> get(User user){
        UserResponse response = userService.get(user);
        return WebResponse.<UserResponse>builder().data(response).build();
    }

    /**
     * Updates the name and/or password of the authenticated user ({@code PATCH /api/users/current}).
     *
     * <p>Only the fields present in the request body are changed; omitted or {@code null} fields keep
     * their current value, and an empty JSON object changes nothing. The {@code user} parameter is
     * resolved from the {@code X-API-TOKEN} header by {@code UserArgumentResolver}. Because it is
     * declared before the body, authentication runs first, so an invalid token is rejected with
     * {@code 401 UNAUTHORIZED} even if the body is also invalid.</p>
     *
     * @param user    the authenticated user, supplied by {@code UserArgumentResolver}
     * @param request the fields to change; validated with {@code @Valid} after authentication
     * @return a {@link WebResponse} whose {@code data} holds the user's username and (possibly new) name
     */
    @PatchMapping(
            path = "/current",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<UserResponse> update(User user, @Valid @RequestBody UpdateUserRequest request){
        UserResponse response = userService.update(user, request);
        return WebResponse.<UserResponse>builder().data(response).build();
    }
}