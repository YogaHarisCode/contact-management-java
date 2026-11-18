package com.yogaharis.contactmanagement.controller;

import com.yogaharis.contactmanagement.dto.LoginUserRequest;
import com.yogaharis.contactmanagement.dto.TokenResponse;
import com.yogaharis.contactmanagement.dto.WebResponse;
import com.yogaharis.contactmanagement.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 *
 * @author Yoga Harisistianugroho
 */
@RestController
@RequiredArgsConstructor
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
            path = "/api/auth/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<TokenResponse> login(@Valid @RequestBody LoginUserRequest request){
        TokenResponse response = authService.login(request);
        return WebResponse.<TokenResponse>builder().data(response).build();
    }
}