package com.yogaharis.contactmanagement.controller;

import com.yogaharis.contactmanagement.dto.ErrorDetail;
import com.yogaharis.contactmanagement.dto.LoginUserRequest;
import com.yogaharis.contactmanagement.dto.TokenResponse;
import com.yogaharis.contactmanagement.dto.WebResponse;
import com.yogaharis.contactmanagement.repository.UserRepository;
import com.yogaharis.contactmanagement.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Web slice test for {@link AuthController} ({@code POST /api/auth/login}).
 *
 * <p>Uses {@code @WebMvcTest}, so only the web layer is loaded: routing, JSON serialization,
 * validation, HTTP status codes and the global exception handler. {@link AuthService} is mocked,
 * and no database is involved.</p>
 *
 * @author Yoga Harisistianugroho
 */
@WebMvcTest(AuthController.class)
@AutoConfigureRestTestClient
public class AuthControllerWebMvcTest {

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private AuthService authService;

    // UserArgumentResolver is part of the WebMvc slice and depends on UserRepository.
    @MockitoBean
    private UserRepository userRepository;

    private static final String VALID_TOKEN = "VALID-TOKEN";
    // Fixed epoch-millisecond value, so assertions do not depend on the current time.
    private static final Long TOKEN_EXPIRED_AT = 1_900_000_000_000L;

    // ---------------------------------------------------------- LOGIN

    /** A valid request is passed to the service and its token is returned in the {@code data} envelope. */
    @Test
    void testLoginSuccess() {
        LoginUserRequest request = LoginUserRequest.builder()
                .username("JohnDoe")
                .password("Rahasia123")
                .build();

        Mockito.when(authService.login(request))
                .thenReturn(new TokenResponse(VALID_TOKEN, TOKEN_EXPIRED_AT));

        WebResponse<TokenResponse> response = postLogin(request)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<TokenResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getErrors());
        assertNotNull(response.getData());
        assertEquals(VALID_TOKEN, response.getData().getToken());
        assertEquals(TOKEN_EXPIRED_AT, response.getData().getTokenExpiredAt());

        Mockito.verify(authService).login(request);
    }

    /** The raw JSON of a success response has the expected field names and no {@code errors} key. */
    @Test
    void testLoginSuccessResponseOmitsErrors() {
        LoginUserRequest request = LoginUserRequest.builder()
                .username("JohnDoe")
                .password("Rahasia123")
                .build();

        Mockito.when(authService.login(request))
                .thenReturn(new TokenResponse(VALID_TOKEN, TOKEN_EXPIRED_AT));

        // The raw body is read as a String because null fields must be absent from the JSON, not just null.
        String body = postLogin(request)
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(body);
        assertTrue(body.contains("\"token\""));
        assertTrue(body.contains("\"tokenExpiredAt\""));
        assertFalse(body.contains("\"errors\""));
    }

    /** Invalid input is rejected with 400 {@code VALIDATION_ERROR} before the service is called. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidLoginRequests")
    void testLoginValidationFailed(String scenario, LoginUserRequest request, String field, String message) {
        WebResponse<Void> response = postLogin(request)
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<Void>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());

        assertNotNull(response.getErrors());
        assertEquals("VALIDATION_ERROR", response.getErrors().getCode());
        assertEquals("Request validation failed", response.getErrors().getMessage());
        assertEquals(field, response.getErrors().getDetails().getFirst().getField());
        assertEquals(message, response.getErrors().getDetails().getFirst().getMessage());

        Mockito.verify(authService, Mockito.never()).login(Mockito.any());
    }

    /** When both fields are invalid, the error details list both of them. */
    @Test
    void testLoginValidationFailedOnMultipleFields() {
        WebResponse<Void> response = postLogin(new LoginUserRequest("", ""))
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<Void>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNotNull(response.getErrors());
        assertEquals("VALIDATION_ERROR", response.getErrors().getCode());

        Set<String> fields = response.getErrors().getDetails().stream()
                .map(ErrorDetail::getField)
                .collect(Collectors.toSet());
        assertEquals(Set.of("username", "password"), fields);

        Mockito.verify(authService, Mockito.never()).login(Mockito.any());
    }

    /** Values exactly on the allowed limits (100 characters, 72 bytes) pass validation. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("validBoundaryLoginRequests")
    void testLoginBoundaryValuesAccepted(String scenario, LoginUserRequest request) {
        Mockito.when(authService.login(request))
                .thenReturn(new TokenResponse(VALID_TOKEN, TOKEN_EXPIRED_AT));

        postLogin(request)
                .expectStatus().isOk();

        Mockito.verify(authService).login(request);
    }

    /** A 401 thrown by the service is returned as the standard error response. */
    @Test
    void testLoginInvalidCredential() {
        LoginUserRequest request = LoginUserRequest.builder()
                .username("JohnDoe")
                .password("passwordsalah")
                .build();

        Mockito.when(authService.login(request))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or password wrong"));

        WebResponse<TokenResponse> response = postLogin(request)
                .expectStatus().isUnauthorized()
                .expectBody(new ParameterizedTypeReference<WebResponse<TokenResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());

        assertNotNull(response.getErrors());
        assertEquals(HttpStatus.UNAUTHORIZED.toString(), response.getErrors().getCode());
        assertEquals("Username or password wrong", response.getErrors().getMessage());

        Mockito.verify(authService).login(request);
    }

    /** The raw JSON of a 401 response has no {@code data} key and no {@code details} key. */
    @Test
    void testLoginInvalidCredentialResponseOmitsNullFields() {
        LoginUserRequest request = LoginUserRequest.builder()
                .username("JohnDoe")
                .password("passwordsalah")
                .build();

        Mockito.when(authService.login(request))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or password wrong"));

        String body = postLogin(request)
                .expectStatus().isUnauthorized()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(body);
        assertTrue(body.contains("\"errors\""));
        assertFalse(body.contains("\"data\""));
        assertFalse(body.contains("\"details\""));
    }

    /** An empty or malformed JSON body gives 400 {@code 400 BAD_REQUEST} and never reaches the service. */
    @ParameterizedTest(name = "[{index}] body = \"{0}\"")
    @ValueSource(strings = {"", "{\"username\": \"JohnDoe\", ", "not-json"})
    void testLoginUnreadableBody(String body) {
        WebResponse<Void> response = postLogin(body)
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<Void>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());

        assertNotNull(response.getErrors());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), response.getErrors().getCode());
        assertNotNull(response.getErrors().getMessage());

        Mockito.verifyNoInteractions(authService);
    }

    /** The endpoint only consumes JSON, so a plain text body gives 415. */
    @Test
    void testLoginUnsupportedMediaType() {
        client.post()
                .uri("/api/auth/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.TEXT_PLAIN)
                .body("username=JohnDoe&password=Rahasia123")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);

        Mockito.verifyNoInteractions(authService);
    }

    /** An unexpected exception gives a generic 500 that does not leak the exception message. */
    @Test
    void testLoginUnexpectedError() {
        LoginUserRequest request = LoginUserRequest.builder()
                .username("JohnDoe")
                .password("Rahasia123")
                .build();

        Mockito.when(authService.login(request))
                .thenThrow(new RuntimeException("database down"));

        WebResponse<Void> response = postLogin(request)
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(new ParameterizedTypeReference<WebResponse<Void>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());

        assertNotNull(response.getErrors());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.toString(), response.getErrors().getCode());
        assertFalse(String.valueOf(response.getErrors().getMessage()).contains("database down"));
    }

    // ---------------------------------------------------------- HELPER

    /**
     * Sends a JSON {@code POST /api/auth/login}.
     *
     * @param body a {@link LoginUserRequest}, or a raw {@code String} to send a malformed body
     * @return the response spec to assert on
     */
    private RestTestClient.ResponseSpec postLogin(Object body) {
        return client.post()
                .uri("/api/auth/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }

    /**
     * Invalid requests as {@code (scenario, request, expected field, expected message)}.
     *
     * @return the arguments for {@code testLoginValidationFailed}
     */
    private static Stream<Arguments> invalidLoginRequests() {
        String over100Chars = "a".repeat(101);
        String over72Bytes = "🚀".repeat(19); // 19 x 4 bytes = 76 bytes

        return Stream.of(
                Arguments.of("username is blank",
                        new LoginUserRequest("", "Rahasia123"),
                        "username", "must not be blank"),
                Arguments.of("username is null",
                        new LoginUserRequest(null, "Rahasia123"),
                        "username", "must not be blank"),
                Arguments.of("username is whitespace only",
                        new LoginUserRequest("   ", "Rahasia123"),
                        "username", "must not be blank"),
                Arguments.of("username is longer than 100 characters",
                        new LoginUserRequest(over100Chars, "Rahasia123"),
                        "username", "size must be between 0 and 100"),
                Arguments.of("password is empty",
                        new LoginUserRequest("JohnDoe", ""),
                        "password", "must not be blank"),
                Arguments.of("password is null",
                        new LoginUserRequest("JohnDoe", null),
                        "password", "must not be blank"),
                Arguments.of("password is whitespace only",
                        new LoginUserRequest("JohnDoe", "   "),
                        "password", "must not be blank"),
                Arguments.of("password is longer than 72 bytes (ASCII)",
                        new LoginUserRequest("JohnDoe", over100Chars),
                        "password", "must not exceed 72 bytes"),
                Arguments.of("password is longer than 72 bytes (multi-byte)",
                        new LoginUserRequest("JohnDoe", over72Bytes),
                        "password", "must not exceed 72 bytes")
        );
    }

    /**
     * Requests exactly on the allowed limits as {@code (scenario, request)}.
     *
     * @return the arguments for {@code testLoginBoundaryValuesAccepted}
     */
    private static Stream<Arguments> validBoundaryLoginRequests() {
        return Stream.of(
                Arguments.of("username is exactly 100 characters",
                        new LoginUserRequest("a".repeat(100), "Rahasia123")),
                Arguments.of("password is exactly 72 bytes (ASCII)",
                        new LoginUserRequest("JohnDoe", "a".repeat(72))),
                Arguments.of("password is exactly 72 bytes (multi-byte)",
                        new LoginUserRequest("JohnDoe", "🚀".repeat(18))) // 18 x 4 bytes = 72 bytes
        );
    }
}