package contactmanagement.controller;

import contactmanagement.entity.User;
import contactmanagement.model.RegisterUserRequest;
import contactmanagement.model.UserResponse;
import contactmanagement.model.WebResponse;
import contactmanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping(
            path = "/api/users/current",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<UserResponse> get(User user){
        UserResponse userResponse = userService.get(user);
        return WebResponse.<UserResponse>builder().data(userResponse).build();
    }
}