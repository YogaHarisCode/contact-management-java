package com.yogaharis.contactmanagement.controller;

import com.yogaharis.contactmanagement.dto.LoginUserRequest;
import com.yogaharis.contactmanagement.dto.TokenResponse;
import com.yogaharis.contactmanagement.dto.WebResponse;
import com.yogaharis.contactmanagement.entity.User;
import com.yogaharis.contactmanagement.repository.UserRepository;
import com.yogaharis.contactmanagement.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the login flow ({@code POST /api/auth/login}), from HTTP request to database.
 *
 * <p>Uses the full Spring context with the {@code test} profile, so it runs against the
 * {@code contact_management_test} database with the real password encoder and system clock.
 * The {@code users} table is emptied before every test.</p>
 *
 * @author Yoga Harisistianugroho
 */
@SpringBootTest
@AutoConfigureRestTestClient
@ActiveProfiles({"test"})
public class AuthControllerTest {

    private static final String RAW_PASSWORD = "Rahasia123";
    // Must match the token validity defined in AuthService.
    private static final Duration TOKEN_VALIDITY = Duration.ofDays(30);

    @Autowired
    private RestTestClient client;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    // ---------------------------------------------------------- LOGIN

    /**
     * A successful login returns a UUID token that expires 30 days after the request, and the
     * token is stored on the user in the database without changing the password hash.
     */
    @Test
    void testLoginSuccess() {
        registerUserDummy("JohnDoe");

        LoginUserRequest request = LoginUserRequest.builder()
                .username("JohnDoe")
                .password(RAW_PASSWORD)
                .build();

        Instant before = Instant.now();
        WebResponse<TokenResponse> response = postLogin(request)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<TokenResponse>>() {
                })
                .returnResult()
                .getResponseBody();
        Instant after = Instant.now();

        assertNotNull(response);
        assertNull(response.getErrors());
        assertNotNull(response.getData());

        String token = response.getData().getToken();
        Long expiredAt = response.getData().getTokenExpiredAt();
        assertDoesNotThrow(() -> UUID.fromString(token));
        // The expiry must fall inside the window between just before and just after the request.
        assertTrue(expiredAt >= before.plus(TOKEN_VALIDITY).toEpochMilli());
        assertTrue(expiredAt <= after.plus(TOKEN_VALIDITY).toEpochMilli());

        User saved = userRepository.findByToken(token).orElseThrow();
        assertEquals("JohnDoe", saved.getUsername());
        assertEquals(expiredAt, saved.getTokenExpiredAt());
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, saved.getPassword()));
    }

    /** An unknown username gives 401 and no user receives a token. */
    @Test
    void testLoginUserNotFound() {
        registerUserDummy("JohnDoe");

        LoginUserRequest request = LoginUserRequest.builder()
                .username("JaneDoe")
                .password(RAW_PASSWORD)
                .build();

        WebResponse<Void> response = postLogin(request)
                .expectStatus().isUnauthorized()
                .expectBody(new ParameterizedTypeReference<WebResponse<Void>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());

        assertNotNull(response.getErrors());
        assertEquals(HttpStatus.UNAUTHORIZED.toString(), response.getErrors().getCode());
        assertEquals("Username or password wrong", response.getErrors().getMessage());
        assertNull(response.getErrors().getDetails());

        assertTrue(userRepository.findAll().stream().allMatch(user -> user.getToken() == null));
    }

    /** A wrong password gives the same 401 as an unknown username and no token is written. */
    @Test
    void testLoginWrongPassword() {
        registerUserDummy("JohnDoe");

        LoginUserRequest request = LoginUserRequest.builder()
                .username("JohnDoe")
                .password("passwordsalah")
                .build();

        WebResponse<Void> response = postLogin(request)
                .expectStatus().isUnauthorized()
                .expectBody(new ParameterizedTypeReference<WebResponse<Void>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());

        assertNotNull(response.getErrors());
        assertEquals(HttpStatus.UNAUTHORIZED.toString(), response.getErrors().getCode());
        assertEquals("Username or password wrong", response.getErrors().getMessage());
        assertNull(response.getErrors().getDetails());

        User user = userRepository.findById("JohnDoe").orElseThrow();
        assertNull(user.getToken());
        assertNull(user.getTokenExpiredAt());
    }

    /** Invalid input is rejected with 400 {@code VALIDATION_ERROR}. */
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
    }

    /** Logging in again replaces the token: the old one is gone from the database, the new one is stored. */
    @Test
    void testLoginTwiceRotatesToken() {
        registerUserDummy("JohnDoe");

        LoginUserRequest request = LoginUserRequest.builder()
                .username("JohnDoe")
                .password(RAW_PASSWORD)
                .build();

        String firstToken = loginAndGetToken(request);
        String secondToken = loginAndGetToken(request);

        assertNotEquals(firstToken, secondToken);
        assertTrue(userRepository.findByToken(firstToken).isEmpty());
        assertEquals("JohnDoe", userRepository.findByToken(secondToken).orElseThrow().getUsername());
    }

    /** A login only touches the row of the user who logged in. */
    @Test
    void testLoginOnlyUpdatesAuthenticatedUser() {
        registerUserDummy("JohnDoe");
        registerUserDummy("JaneDoe");

        LoginUserRequest request = LoginUserRequest.builder()
                .username("JohnDoe")
                .password(RAW_PASSWORD)
                .build();

        String token = loginAndGetToken(request);

        assertEquals(token, userRepository.findById("JohnDoe").orElseThrow().getToken());

        User other = userRepository.findById("JaneDoe").orElseThrow();
        assertNull(other.getToken());
        assertNull(other.getTokenExpiredAt());
    }

    // ---------------------------------------------------------- LOGOUT

    @Test
    void testLogoutSuccess() {
        saveUserWithToken("JohnDoe", "Valid-Token", Instant.now().plus(Duration.ofDays(1)).toEpochMilli());

        WebResponse<String> response = deleteLogout("Valid-Token")
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<String>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals("OK", response.getData());
        assertNull(response.getErrors());

        User userDb = userRepository.findById("JohnDoe").orElseThrow();
        assertNull(userDb.getToken());
        assertNull(userDb.getTokenExpiredAt());
        assertEquals("John Doe", userDb.getName());
        assertEquals("Hash-Password", userDb.getPassword());
    }

    @Test
    void testLogoutInvalidatesTokenOnProtectedEndpoint() {
        registerUserDummy("JohnDoe");
        String token = loginAndGetToken(loginRequest());

        getCurrentUser(token).expectStatus().isOk();
        deleteLogout(token).expectStatus().isOk();

        assertUnauthorized(getCurrentUser(token));
    }

    @Test
    void testLogoutThenLoginIssuesNewValidToken() {
        registerUserDummy("JohnDoe");
        LoginUserRequest request = loginRequest();

        String firstToken = loginAndGetToken(request);
        deleteLogout(firstToken).expectStatus().isOk();
        String secondToken = loginAndGetToken(request);

        assertNotEquals(firstToken, secondToken);
        getCurrentUser(secondToken).expectStatus().isOk();
        assertUnauthorized(getCurrentUser(firstToken));
    }

    @Test
    void testLogoutTwiceRejectsSecondAttempt() {
        registerUserDummy("JohnDoe");
        String token = loginAndGetToken(loginRequest());

        deleteLogout(token).expectStatus().isOk();

        assertUnauthorized(deleteLogout(token));
    }

    @Test
    void testLogoutOnlyClearsAuthenticatedUser() {
        long expiredAt = Instant.now().plus(Duration.ofDays(1)).toEpochMilli();
        saveUserWithToken("JohnDoe", "John-Token", expiredAt);
        saveUserWithToken("JaneDoe", "Jane-Token", expiredAt);

        deleteLogout("John-Token").expectStatus().isOk();

        User john = userRepository.findById("JohnDoe").orElseThrow();
        assertNull(john.getToken());
        assertNull(john.getTokenExpiredAt());

        User jane = userRepository.findById("JaneDoe").orElseThrow();
        assertEquals("Jane-Token", jane.getToken());
        assertEquals(expiredAt, jane.getTokenExpiredAt());
    }

    @Test
    void testLogoutWithReplacedTokenKeepsNewToken() {
        registerUserDummy("JohnDoe");
        LoginUserRequest request = loginRequest();

        String firstToken = loginAndGetToken(request);
        String secondToken = loginAndGetToken(request);

        assertUnauthorized(deleteLogout(firstToken));

        assertEquals(secondToken, userRepository.findById("JohnDoe").orElseThrow().getToken());
    }

    @Test
    void testLogoutWithStaleUserDoesNotRevokeNewToken() {
        registerUserDummy("JohnDoe");
        LoginUserRequest request = loginRequest();

        String firstToken = loginAndGetToken(request);
        User staleUser = userRepository.findByToken(firstToken).orElseThrow();
        String secondToken = loginAndGetToken(request);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> authService.logout(staleUser));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        User userDb = userRepository.findById("JohnDoe").orElseThrow();
        assertEquals(secondToken, userDb.getToken());
        assertNotNull(userDb.getTokenExpiredAt());
    }

    @Test
    void testLogoutWithoutHeader() {
        assertUnauthorized(deleteLogoutWithoutHeader());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void testLogoutBlankToken(String token) {
        assertUnauthorized(deleteLogout(token));
    }

    @Test
    void testLogoutUnknownToken() {
        long expiredAt = Instant.now().plus(Duration.ofDays(1)).toEpochMilli();
        saveUserWithToken("JohnDoe", "John-Token", expiredAt);

        assertUnauthorized(deleteLogout("Unknown-Token"));

        assertEquals("John-Token", userRepository.findById("JohnDoe").orElseThrow().getToken());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidExpirations")
    void testLogoutWithInvalidExpiration(String scenario, Long tokenExpiredAt) {
        saveUserWithToken("JohnDoe", "Valid-Token", tokenExpiredAt);

        assertUnauthorized(deleteLogout("Valid-Token"));

        User userDb = userRepository.findById("JohnDoe").orElseThrow();
        assertEquals("Valid-Token", userDb.getToken());
        assertEquals(tokenExpiredAt, userDb.getTokenExpiredAt());
    }

    // ---------------------------------------------------------- HELPER

    private static Stream<Arguments> invalidExpirations() {
        return Stream.of(
                Arguments.of("token is expired", Instant.now().minus(Duration.ofHours(1)).toEpochMilli()),
                Arguments.of("token has no expiry", null)
        );
    }

    private LoginUserRequest loginRequest() {
        return LoginUserRequest.builder()
                .username("JohnDoe")
                .password(RAW_PASSWORD)
                .build();
    }

    private void saveUserWithToken(String username, String token, Long tokenExpiredAt) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("Hash-Password");
        user.setName("John Doe");
        user.setToken(token);
        user.setTokenExpiredAt(tokenExpiredAt);
        userRepository.save(user);
    }

    private RestTestClient.ResponseSpec deleteLogout(String token) {
        return client.delete()
                .uri("/api/auth/logout")
                .header("X-API-TOKEN", token)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
    }

    private RestTestClient.ResponseSpec deleteLogoutWithoutHeader() {
        return client.delete()
                .uri("/api/auth/logout")
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
    }

    private RestTestClient.ResponseSpec getCurrentUser(String token) {
        return client.get()
                .uri("/api/users/current")
                .header("X-API-TOKEN", token)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
    }

    private void assertUnauthorized(RestTestClient.ResponseSpec spec) {
        WebResponse<Void> response = spec
                .expectStatus().isUnauthorized()
                .expectBody(new ParameterizedTypeReference<WebResponse<Void>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());
        assertNotNull(response.getErrors());
        assertEquals(HttpStatus.UNAUTHORIZED.toString(), response.getErrors().getCode());
        assertEquals("Unauthorized", response.getErrors().getMessage());
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
                        new LoginUserRequest("", RAW_PASSWORD),
                        "username", "must not be blank"),
                Arguments.of("username is null",
                        new LoginUserRequest(null, RAW_PASSWORD),
                        "username", "must not be blank"),
                Arguments.of("username is longer than 100 characters",
                        new LoginUserRequest(over100Chars, RAW_PASSWORD),
                        "username", "size must be between 0 and 100"),
                Arguments.of("password is empty",
                        new LoginUserRequest("JohnDoe", ""),
                        "password", "must not be blank"),
                Arguments.of("password is null",
                        new LoginUserRequest("JohnDoe", null),
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
     * Saves a user whose password is {@code RAW_PASSWORD}, hashed with the real encoder.
     * It is saved directly through the repository, so the test does not depend on the register endpoint.
     *
     * @param username the username of the user to save
     */
    private void registerUserDummy(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(RAW_PASSWORD));
        user.setName("John Doe");
        userRepository.save(user);
    }

    /**
     * Logs in and expects a successful response.
     *
     * @param request the login request to send
     * @return the token returned by the API
     */
    private String loginAndGetToken(LoginUserRequest request) {
        WebResponse<TokenResponse> response = postLogin(request)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<TokenResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNotNull(response.getData());
        return response.getData().getToken();
    }

    /**
     * Sends a JSON {@code POST /api/auth/login}.
     *
     * @param request the login request to send
     * @return the response spec to assert on
     */
    private RestTestClient.ResponseSpec postLogin(LoginUserRequest request) {
        return client.post()
                .uri("/api/auth/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange();
    }
}