package contactmanagement.controller;

import contactmanagement.entity.User;
import contactmanagement.model.RegisterUserRequest;
import contactmanagement.model.UpdateUserRequest;
import contactmanagement.model.UserResponse;
import contactmanagement.model.WebResponse;
import contactmanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing user management endpoints under {@code /api/users}.
 *
 * <p>The controller only handles HTTP concerns (mapping, validation trigger, response envelope);
 * all business rules live in {@link UserService}.
 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Registers a new user. This endpoint is public and requires no authentication.
     *
     * @param request the registration data; validated before the method is invoked
     * @return a {@link WebResponse} with HTTP 200 containing the created user's username and name
     */
    @PostMapping(
            path = "/api/users",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<UserResponse> register(@Valid @RequestBody RegisterUserRequest request){
        UserResponse response = userService.register(request);
        return WebResponse.<UserResponse>builder().data(response).build();
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * <p>Requires the {@code X-API-TOKEN} header; authentication is performed by
     * {@link contactmanagement.resolver.UserArgumentResolver} before this method is invoked.
     *
     * @param user the authenticated user, injected by the argument resolver (not read from the
     *             request body)
     * @return a {@link WebResponse} with HTTP 200 containing the user's username and name
     */
    @GetMapping(
            path = "/api/users/current",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<UserResponse> get(User user){
        // The User parameter is supplied by UserArgumentResolver, which also enforces authentication.
        UserResponse userResponse = userService.get(user);
        return WebResponse.<UserResponse>builder().data(userResponse).build();
    }

    /**
     * Updates the name and/or password of the currently authenticated user.
     *
     * <p>Requires the {@code X-API-TOKEN} header; authentication is performed by
     * {@link contactmanagement.resolver.UserArgumentResolver} before the body is validated, so
     * a missing or invalid token always results in 401, even when the body is also invalid.
     *
     * @param user    the authenticated user, injected by the argument resolver (not read from
     *                the request body)
     * @param request the fields to change; every field is optional and validated before the
     *                method is invoked
     * @return a {@link WebResponse} with HTTP 200 containing the updated username and name
     */
    @PatchMapping(
            path = "/api/users/current",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<UserResponse> update (User user,@Valid @RequestBody UpdateUserRequest request){
        UserResponse response = userService.update(user, request);
        return WebResponse.<UserResponse>builder().data(response).build();
    }
}