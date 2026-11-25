package com.yogaharis.contactmanagement.controller;

import com.yogaharis.contactmanagement.dto.LoginUserRequest;
import com.yogaharis.contactmanagement.dto.TokenResponse;
import com.yogaharis.contactmanagement.dto.WebResponse;
import com.yogaharis.contactmanagement.entity.User;
import com.yogaharis.contactmanagement.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 *
 * @author Yoga Harisistianugroho
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Logs a user in: {@code POST /api/auth/login}. Consumes and produces JSON.
     *
     * <p>Errors are turned into the standard error response by the global exception handler:
     * {@code 400} when validation fails or the body is missing or malformed, and
     * {@code 401} when the username or password is wrong.</p>
     *
     * @param request the login credentials; validated with {@code @Valid} before the service is called
     * @return the standard response envelope whose {@code data} holds the token and its expiry
     */
    @PostMapping(
            path = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<TokenResponse> login(@Valid @RequestBody LoginUserRequest request){
        TokenResponse response = authService.login(request);
        return WebResponse.<TokenResponse>builder().data(response).build();
    }

    /**
     * Logs the current user out: {@code DELETE /api/auth/logout}. Produces JSON and takes no request body.
     *
     * <p>The user is authenticated from the {@code X-API-TOKEN} header by {@code UserArgumentResolver},
     * because the method declares a {@link User} parameter. On success the user's token is revoked and
     * stops working immediately. The standard error response is returned with {@code 401} when the header
     * is missing or blank, the token is unknown, expired or has no expiry, the user no longer exists, or
     * the token was replaced by a newer login.</p>
     *
     * @param user the authenticated user, resolved from the {@code X-API-TOKEN} header
     * @return the standard response envelope whose {@code data} is the text {@code "OK"}
     */
    @DeleteMapping(
            path = "/logout",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> logout(User user){
        authService.logout(user);
        return WebResponse.<String>builder().data("OK").build();
    }
}