package com.yogaharis.contactmanagement.controller;

import com.yogaharis.contactmanagement.dto.RegisterUserRequest;
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

    @GetMapping(
            path = "/current",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<UserResponse> get(User user){
        UserResponse response = userService.get(user);
        return WebResponse.<UserResponse>builder().data(response).build();
    }
}