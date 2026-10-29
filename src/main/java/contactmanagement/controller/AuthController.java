package contactmanagement.controller;

import contactmanagement.entity.User;
import contactmanagement.model.LoginResponse;
import contactmanagement.model.LoginUserRequest;
import contactmanagement.model.WebResponse;
import contactmanagement.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing authentication endpoints under {@code /api/auth}.
 *
 * <p>The controller only handles HTTP concerns (mapping, validation trigger, response envelope);
 * all business rules live in {@link AuthService}.
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user. This endpoint is public and requires no token.
     *
     * @param request the login credentials; validated before the method is invoked
     * @return a {@link WebResponse} with HTTP 200 containing the token and its expiration time
     */
    @PostMapping(
            path = "/api/auth/login",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<LoginResponse> login(@Valid @RequestBody LoginUserRequest request){
        LoginResponse response = authService.login(request);
        return WebResponse.<LoginResponse>builder().data(response).build();
    }

    /**
     * Logs out the current user by invalidating the token sent in the request.
     *
     * <p>This endpoint requires the {@code X-API-TOKEN} header. It is protected because the
     * method declares a {@link User} parameter, which {@code UserArgumentResolver} resolves
     * before the method runs; a missing, unknown, or expired token returns
     * {@code 401 Unauthorized} and this method is never invoked.
     *
     * @param user the authenticated user, resolved from the {@code X-API-TOKEN} header
     * @return a {@link WebResponse} with HTTP 200 whose {@code data} is {@code "OK"}
     */
    @DeleteMapping(
            path = "/api/auth/logout",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> logout(User user){
        authService.logout(user);
        return WebResponse.<String>builder().data("OK").build();
    }
}