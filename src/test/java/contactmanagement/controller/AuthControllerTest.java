package contactmanagement.controller;

import contactmanagement.entity.User;
import contactmanagement.model.LoginResponse;
import contactmanagement.model.LoginUserRequest;
import contactmanagement.model.WebResponse;
import contactmanagement.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link AuthController} covering the login-user feature.
 *
 * <p>Runs against the real datasource configured in {@code application.properties} and clears
 * the {@code users} table around every test, so point it to a dedicated test database.
 */
@SpringBootTest
@AutoConfigureRestTestClient
class AuthControllerTest {

    private static final String USERNAME = "test";
    private static final String PASSWORD = "test1234";
    private static final String INVALID_CREDENTIALS = "Username or password wrong";
    private static final long TOKEN_TTL_MILLIS = Duration.ofDays(30).toMillis();

    @Autowired
    private RestTestClient client;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = new User();
        user.setUsername(USERNAME);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setName("test");
        userRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    /** Valid credentials return 200, a token, and an expiry about 30 days ahead; both are persisted. */
    @Test
    void testLoginSuccess() {
        long before = System.currentTimeMillis();

        WebResponse<LoginResponse> response = postLoginOk(login(USERNAME, PASSWORD));

        long after = System.currentTimeMillis();

        assertNotNull(response);
        assertNull(response.getErrors());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getToken());
        assertFalse(response.getData().getToken().isBlank());

        Long expiredAt = response.getData().getTokenExpiredAt();
        assertNotNull(expiredAt);
        assertTrue(expiredAt >= before + TOKEN_TTL_MILLIS);
        assertTrue(expiredAt <= after + TOKEN_TTL_MILLIS);

        User saved = userRepository.findById(USERNAME).orElseThrow();
        assertEquals(response.getData().getToken(), saved.getToken());
        assertEquals(expiredAt, saved.getTokenExpiredAt());
        assertTrue(passwordEncoder.matches(PASSWORD, saved.getPassword()));
    }

    /** The raw JSON response must not contain any password field or value. */
    @Test
    void testLoginResponseDoesNotExposePassword() {
        String rawBody = client.post()
                .uri("/api/auth/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(login(USERNAME, PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(rawBody);
        assertFalse(rawBody.contains("password"));
        assertFalse(rawBody.contains(PASSWORD));
    }

    /** A second login issues a new token that replaces the previous one. */
    @Test
    void testLoginTwiceIssuesNewToken() {
        String firstToken = postLoginOk(login(USERNAME, PASSWORD)).getData().getToken();
        String secondToken = postLoginOk(login(USERNAME, PASSWORD)).getData().getToken();

        assertNotEquals(firstToken, secondToken);
        assertEquals(secondToken, userRepository.findById(USERNAME).orElseThrow().getToken());
    }

    /** A wrong password returns 401 and must not change the stored token. */
    @Test
    void testLoginWrongPassword() {
        givenUserHasToken("existing-token", 123L);

        WebResponse<LoginResponse> response = postLoginUnauthorized(login(USERNAME, "test1233"));

        assertNotNull(response);
        assertEquals(INVALID_CREDENTIALS, response.getErrors());
        assertNull(response.getData());

        User unchanged = userRepository.findById(USERNAME).orElseThrow();
        assertEquals("existing-token", unchanged.getToken());
        assertEquals(123L, unchanged.getTokenExpiredAt());
    }

    /** An unknown username returns 401 with the same message as a wrong password. */
    @Test
    void testLoginUserNotFound() {
        WebResponse<LoginResponse> response = postLoginUnauthorized(login("testis", "test1233"));

        assertNotNull(response);
        assertEquals(INVALID_CREDENTIALS, response.getErrors());
        assertNull(response.getData());
    }

    /** Wrong password and unknown user must be indistinguishable to the client. */
    @Test
    void testLoginFailureMessagesAreIdentical() {
        WebResponse<LoginResponse> wrongPassword = postLoginUnauthorized(login(USERNAME, "wrong-password"));
        WebResponse<LoginResponse> unknownUser = postLoginUnauthorized(login("unknown-user", PASSWORD));

        assertEquals(wrongPassword.getErrors(), unknownUser.getErrors());
    }

    /** A username that differs only in letter case still matches on MySQL's default collation. */
    @Test
    void testLoginDoesNotIssueTokenForOtherUsers() {
        postLoginUnauthorized(login("testis", PASSWORD));

        assertNull(userRepository.findById(USERNAME).orElseThrow().getToken());
    }

    /** Every invalid payload returns 400 with an error message and issues no token. */
    @ParameterizedTest
    @MethodSource("invalidRequests")
    void testLoginInvalid(LoginUserRequest request) {
        WebResponse<LoginResponse> response = postLoginBadRequest(request);

        assertNotNull(response);
        assertNotNull(response.getErrors());
        assertNull(response.getData());
        assertNull(userRepository.findById(USERNAME).orElseThrow().getToken());
    }

    /** Supplies one invalid payload per constraint: blank, null, and too long values. */
    static Stream<LoginUserRequest> invalidRequests() {
        String tooLong = "a".repeat(101);
        return Stream.of(
                login("", PASSWORD),
                login(null, PASSWORD),
                login("   ", PASSWORD),
                login(tooLong, PASSWORD),
                login(USERNAME, ""),
                login(USERNAME, null),
                login(USERNAME, "   "),
                login(USERNAME, tooLong)
        );
    }

    /** A body that is not valid JSON returns 400 with the generic message. */
    @Test
    void testLoginMalformedJson() {
        WebResponse<LoginResponse> response = client.post()
                .uri("/api/auth/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\": ")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<LoginResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals("Malformed or missing request body", response.getErrors());
    }

    /** A request without any body returns 400 with the generic message. */
    @Test
    void testLoginMissingBody() {
        WebResponse<LoginResponse> response = client.post()
                .uri("/api/auth/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<LoginResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals("Malformed or missing request body", response.getErrors());
    }

    /** Shortcut for building a login request. */
    private static LoginUserRequest login(String username, String password) {
        return LoginUserRequest.builder()
                .username(username)
                .password(password)
                .build();
    }

    /** Stores a known token on the test user so tests can verify it stays untouched. */
    private void givenUserHasToken(String token, long expiredAt) {
        User user = userRepository.findById(USERNAME).orElseThrow();
        user.setToken(token);
        user.setTokenExpiredAt(expiredAt);
        userRepository.save(user);
    }

    /** Posts to {@code /api/auth/login}, asserts HTTP 200, and returns the parsed response. */
    private WebResponse<LoginResponse> postLoginOk(LoginUserRequest request) {
        return client.post()
                .uri("/api/auth/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<LoginResponse>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    /** Posts to {@code /api/auth/login}, asserts HTTP 401, and returns the parsed response. */
    private WebResponse<LoginResponse> postLoginUnauthorized(LoginUserRequest request) {
        return client.post()
                .uri("/api/auth/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(new ParameterizedTypeReference<WebResponse<LoginResponse>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    /** Posts to {@code /api/auth/login}, asserts HTTP 400, and returns the parsed response. */
    private WebResponse<LoginResponse> postLoginBadRequest(LoginUserRequest request) {
        return client.post()
                .uri("/api/auth/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<LoginResponse>>() {
                })
                .returnResult()
                .getResponseBody();
    }
}