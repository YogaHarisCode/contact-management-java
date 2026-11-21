package com.yogaharis.contactmanagement.controller;

import com.yogaharis.contactmanagement.dto.*;
import com.yogaharis.contactmanagement.entity.User;
import com.yogaharis.contactmanagement.repository.UserRepository;
import com.yogaharis.contactmanagement.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the user endpoints (register, get current user, and update user), running the full
 * application context against the {@code contact_management_test} database.
 *
 * @author Yoga Harisistianugroho
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureRestTestClient
public class UserControllerTest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    private static final String USERNAME = "JhonDoe";
    private static final String RAW_PASSWORD = "Rahasia123";
    private static final String NAME = "Jhon Doe";
    private static final String VALID_TOKEN = "Valid-Token";
    private static final String INVALID_TOKEN = "Invalid-Token";
    private static final String HEADER = "X-API-TOKEN";
    private static final String DUMMY_HASH = "hash-password";
    private static final String NEW_NAME = "Jhon Doe Updated";
    private static final String NEW_PASSWORD = "NewPassword123";

    // The users table is emptied before and after every test so tests stay independent.
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }


    // ---------------------------------------------------------- REGISTER
    @Test
    void testRegisterSuccess() {
        RegisterUserRequest request = validRegisterRequest();

        WebResponse<UserResponse> response = post(request)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);

        assertNotNull(response.getData());
        assertEquals(request.getUsername(), response.getData().getUsername());
        assertEquals(request.getName(), response.getData().getName());

        assertNull(response.getErrors());

        User userDb = userRepository.findById("JhonDoe").orElse(null);

        assertNotNull(userDb);

        assertEquals(request.getUsername(), userDb.getUsername());
        assertEquals(request.getName(), userDb.getName());

        assertNotEquals(request.getPassword(), userDb.getPassword());

        assertTrue(passwordEncoder.matches(request.getPassword(), userDb.getPassword()));
    }

    @Test
    void testRegisterDuplicateUsername() {
        User savedUser = new User();
        savedUser.setUsername("JhonDoe");
        savedUser.setPassword("hash-password");
        savedUser.setName("Jhon Doe");
        userRepository.save(savedUser);

        RegisterUserRequest request = validRegisterRequest();

        WebResponse<Void> response = post(request)
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(new ParameterizedTypeReference<WebResponse<Void>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);

        assertNull(response.getData());

        assertNotNull(response.getErrors());
        assertEquals(HttpStatus.CONFLICT.toString(), response.getErrors().getCode());
        assertEquals("Username already exists", response.getErrors().getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({"invalidRegisterRequests"})
    void testRegisterValidationInvalid(String scenario, RegisterUserRequest request, String invalidField, String invalidMessage) {
        WebResponse<Void> response = post(request)
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
        // anyMatch: one invalid value can violate several rules at once (e.g. blank and too short).
        assertTrue(
                response.getErrors().getDetails().stream()
                        .anyMatch(detail ->
                                detail.getField().equals(invalidField)
                                        && detail.getMessage().equals(invalidMessage)
                        )
        );
    }

    // ---------------------------------------------------------- GET

    /** The user is saved directly in the database with a known token, bypassing register and login. */
    @Test
    void testGetCurrentUserSuccess() {
        User user = registeredUser();

        WebResponse<UserResponse> response = getCurrent(VALID_TOKEN)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);

        assertNull(response.getErrors());

        assertNotNull(response.getData());
        assertEquals(user.getUsername(), response.getData().getUsername());
        assertEquals(user.getName(), response.getData().getName());
    }

    /** With two users stored, each token must return its own owner and never another user's data. */
    @Test
    void testGetCurrentUserReturnsOwnerOfToken() {
        registeredUser();
        registeredUser("AnotherUser", "Another User", "Another-Token", Instant.now().plus(Duration.ofDays(1)).toEpochMilli());

        WebResponse<UserResponse> response = getCurrent("Another-Token")
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);

        assertNull(response.getErrors());

        assertNotNull(response.getData());
        assertEquals("AnotherUser", response.getData().getUsername());
        assertEquals("Another User", response.getData().getName());
    }

    /** End-to-end flow with a real token: register, login, then get current user. */
    @Test
    void testGetCurrentUserAfterRegisterAndLogin() {
        post(validRegisterRequest()).expectStatus().isOk();

        String token = login(USERNAME, RAW_PASSWORD);

        WebResponse<UserResponse> response = getCurrent(token)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);

        assertNull(response.getErrors());

        assertNotNull(response.getData());
        assertEquals(USERNAME, response.getData().getUsername());
        assertEquals(NAME, response.getData().getName());
    }

    /** A user has one token at a time: logging in again invalidates the previous token. */
    @Test
    void testGetCurrentUserOldTokenAfterRelogin() {
        post(validRegisterRequest()).expectStatus().isOk();

        String oldToken = login(USERNAME, RAW_PASSWORD);
        String newToken = login(USERNAME, RAW_PASSWORD);

        assertNotEquals(oldToken, newToken);

        assertUnauthorized(getCurrent(oldToken));
        getCurrent(newToken).expectStatus().isOk();
    }

    @Test
    void testGetCurrentUserMissingHeader() {
        assertUnauthorized(getCurrent(null));
    }

    @Test
    void testGetCurrentUserTokenNotFound() {
        registeredUser();

        assertUnauthorized(getCurrent(INVALID_TOKEN));
    }

    @Test
    void testGetCurrentUserTokenExpired() {
        registeredUser(USERNAME, NAME, VALID_TOKEN, Instant.now().minus(Duration.ofSeconds(1)).toEpochMilli());

        assertUnauthorized(getCurrent(VALID_TOKEN));
    }

    /** A stored token without an expiry time is treated as invalid. */
    @Test
    void testGetCurrentUserNullTokenExpiredAt() {
        registeredUser(USERNAME, NAME, VALID_TOKEN, null);

        assertUnauthorized(getCurrent(VALID_TOKEN));
    }

    // ---------------------------------------------------------- UPDATE
    /** Only the name changes; the stored password hash and token fields stay exactly as they were. */
    @Test
    void testUpdateUserNameOnlySuccess() {
        // arrange
        User user = registeredUser();

        // act
        WebResponse<UserResponse> response = patchOk(VALID_TOKEN, UpdateUserRequest.builder()
                .name(NEW_NAME)
                .build());

        // assert
        assertEquals(USERNAME, response.getData().getUsername());
        assertEquals(NEW_NAME, response.getData().getName());

        User userDb = findUser(USERNAME);

        assertEquals(NEW_NAME, userDb.getName());
        assertEquals(user.getPassword(), userDb.getPassword());
        assertEquals(user.getToken(), userDb.getToken());
        assertEquals(user.getTokenExpiredAt(), userDb.getTokenExpiredAt());
    }

    /** Uses the real register and login flow so the stored password is a real BCrypt hash. */
    @Test
    void testUpdateUserPasswordOnlySuccess() {
        // arrange
        post(validRegisterRequest()).expectStatus().isOk();
        String token = login(USERNAME, RAW_PASSWORD);
        User before = findUser(USERNAME);

        // act
        WebResponse<UserResponse> response = patchOk(token, UpdateUserRequest.builder()
                .password(NEW_PASSWORD)
                .build());

        // assert
        assertEquals(USERNAME, response.getData().getUsername());
        assertEquals(NAME, response.getData().getName());

        User userDb = findUser(USERNAME);

        assertNotEquals(NEW_PASSWORD, userDb.getPassword());
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, userDb.getPassword()));
        assertEquals(NAME, userDb.getName());
        assertEquals(before.getToken(), userDb.getToken());
        assertEquals(before.getTokenExpiredAt(), userDb.getTokenExpiredAt());

        loginSpec(USERNAME, RAW_PASSWORD).expectStatus().isUnauthorized();
        assertNotNull(login(USERNAME, NEW_PASSWORD));
    }

    /** The stored dummy hash is replaced by a BCrypt hash of the new password, which then works for login; the token is not changed. */
    @Test
    void testUpdateUserBothFieldSuccess() {
        // arrange
        User user = registeredUser();

        // act
        WebResponse<UserResponse> response = patchOk(VALID_TOKEN, UpdateUserRequest.builder()
                .name(NEW_NAME)
                .password(NEW_PASSWORD)
                .build());

        // assert
        assertEquals(USERNAME, response.getData().getUsername());
        assertEquals(NEW_NAME, response.getData().getName());

        User userDb = findUser(USERNAME);

        assertEquals(NEW_NAME, userDb.getName());
        assertNotEquals(user.getPassword(), userDb.getPassword());
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, userDb.getPassword()));
        assertEquals(user.getToken(), userDb.getToken());
        assertEquals(user.getTokenExpiredAt(), userDb.getTokenExpiredAt());

        assertNotNull(login(USERNAME, NEW_PASSWORD));
    }

    /** An empty request returns the current data and leaves the stored row untouched. */
    @Test
    void testUpdateUserNoOpIntegration() {
        // arrange
        User user = registeredUser();

        // act
        WebResponse<UserResponse> response = patchOk(VALID_TOKEN, UpdateUserRequest.builder().build());

        // assert
        assertEquals(USERNAME, response.getData().getUsername());
        assertEquals(NAME, response.getData().getName());

        assertUnchanged(user);
    }

    /** Updating through one user's token must not touch another user's row. */
    @Test
    void testUpdateOnlyChangesAuthenticatedUser() {
        // arrange
        registeredUser();
        User other = registeredUser("AnotherUser", "Another User", "Another-Token", Instant.now().plus(Duration.ofDays(1)).toEpochMilli());

        // act
        patchOk(VALID_TOKEN, UpdateUserRequest.builder()
                .name(NEW_NAME)
                .password(NEW_PASSWORD)
                .build());

        // assert
        assertEquals(NEW_NAME, findUser(USERNAME).getName());
        assertUnchanged(other);
    }

    /** The DTO has no username or token fields, so those JSON keys must never reach the database. */
    @Test
    void testUpdateIgnoresIdentityAndTokenFields() {
        // arrange
        User user = registeredUser();

        // act
        patchOk(VALID_TOKEN, Map.of(
                "name", NEW_NAME,
                "username", "Hacker",
                "token", "Stolen-Token",
                "tokenExpiredAt", 1L
        ));

        // assert
        assertTrue(userRepository.findById("Hacker").isEmpty());

        User userDb = findUser(USERNAME);

        assertEquals(NEW_NAME, userDb.getName());
        assertEquals(user.getToken(), userDb.getToken());
        assertEquals(user.getTokenExpiredAt(), userDb.getTokenExpiredAt());
    }

    /** The longest allowed name and a 72-byte multi-byte password are stored, and the new password works for login. */
    @Test
    void testUpdateBoundaryValuesPersisted() {
        // arrange
        post(validRegisterRequest()).expectStatus().isOk();
        String token = login(USERNAME, RAW_PASSWORD);

        String maxName = "a".repeat(100);
        String maxBytesPassword = "🚀".repeat(18); // 18 x 4 bytes = 72 bytes

        // act
        patchOk(token, UpdateUserRequest.builder()
                .name(maxName)
                .password(maxBytesPassword)
                .build());

        // assert
        User userDb = findUser(USERNAME);

        assertEquals(maxName, userDb.getName());
        assertTrue(passwordEncoder.matches(maxBytesPassword, userDb.getPassword()));
        assertNotNull(login(USERNAME, maxBytesPassword));
    }

    /**
     * Regression test: a login that happens after the request was authenticated must not be undone by the
     * update. The user passed to the service is loaded before that login, so it holds the old token.
     */
    @Test
    void testUpdateDoesNotOverwriteTokenIssuedAfterAuthentication() {
        // arrange
        post(validRegisterRequest()).expectStatus().isOk();

        String oldToken = login(USERNAME, RAW_PASSWORD);
        User staleUser = userRepository.findByToken(oldToken).orElseThrow();
        String newToken = login(USERNAME, RAW_PASSWORD);

        // act
        userService.update(staleUser, UpdateUserRequest.builder()
                .name(NEW_NAME)
                .build());

        // assert
        User userDb = findUser(USERNAME);

        assertEquals(NEW_NAME, userDb.getName());
        assertEquals(newToken, userDb.getToken());

        getCurrent(newToken).expectStatus().isOk();
        assertUnauthorized(getCurrent(oldToken));
    }

    /** No usable token: 401, and the stored user is unchanged. */
    @ParameterizedTest(name = "token = [{0}]")
    @NullAndEmptySource
    void testUpdateMissingToken(String token) {
        User user = registeredUser();

        assertUnauthorized(patchCurrent(token, UpdateUserRequest.builder().name(NEW_NAME).build()));

        assertUnchanged(user);
    }

    /** A token that no user owns: 401, and the stored user is unchanged. */
    @Test
    void testUpdateTokenNotFound() {
        User user = registeredUser();

        assertUnauthorized(patchCurrent(INVALID_TOKEN, UpdateUserRequest.builder().name(NEW_NAME).build()));

        assertUnchanged(user);
    }

    /** A token that expired one second ago: 401, and the stored user is unchanged. */
    @Test
    void testUpdateTokenExpired() {
        User user = registeredUser(USERNAME, NAME, VALID_TOKEN, Instant.now().minus(Duration.ofSeconds(1)).toEpochMilli());

        assertUnauthorized(patchCurrent(VALID_TOKEN, UpdateUserRequest.builder().name(NEW_NAME).build()));

        assertUnchanged(user);
    }

    /** A request with one valid and one invalid field is rejected as a whole; nothing is partially saved. */
    @ParameterizedTest(name = "{0}")
    @MethodSource({"invalidUpdateRequests"})
    void testUpdateValidationFailedChangesNothing(String scenario, UpdateUserRequest request) {
        // arrange
        User user = registeredUser();

        // act
        WebResponse<Void> response = patchCurrent(VALID_TOKEN, request)
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<Void>>() {
                })
                .returnResult()
                .getResponseBody();

        // assert
        assertNotNull(response);
        assertNull(response.getData());
        assertNotNull(response.getErrors());
        assertEquals("VALIDATION_ERROR", response.getErrors().getCode());
        assertEquals("Request validation failed", response.getErrors().getMessage());
        assertNotNull(response.getErrors().getDetails());

        assertUnchanged(user);
    }


    // ---------------------------------------------------------- HELPER

    /** Requests that mix a valid field with an invalid one, to show a rejected request saves nothing. */
    private static Stream<Arguments> invalidUpdateRequests(){
        return Stream.of(
                Arguments.of("valid name, password too short",
                        UpdateUserRequest.builder().name(NEW_NAME).password("1234567").build()),
                Arguments.of("valid name, password longer than 72 bytes",
                        UpdateUserRequest.builder().name(NEW_NAME).password("🚀".repeat(19)).build()),
                Arguments.of("blank name, valid password",
                        UpdateUserRequest.builder().name("   ").password(NEW_PASSWORD).build()),
                Arguments.of("name longer than 100 characters, valid password",
                        UpdateUserRequest.builder().name("a".repeat(101)).password(NEW_PASSWORD).build())
        );
    }

    /** Saves {@code USERNAME} directly in the database with {@code VALID_TOKEN}, valid for one day. */
    private User registeredUser(){
        return registeredUser(USERNAME, NAME, VALID_TOKEN, Instant.now().plus(Duration.ofDays(1)).toEpochMilli());
    }

    /** Saves a user directly in the database with the given token and expiry. The password is a dummy hash. */
    private User registeredUser(String username, String name, String token, Long tokenExpiredAt){
        User user = new User();
        user.setUsername(username);
        user.setPassword(DUMMY_HASH);
        user.setName(name);
        user.setToken(token);
        user.setTokenExpiredAt(tokenExpiredAt);
        return userRepository.save(user);
    }

    /** Loads a user straight from the database, failing the test if it is missing. */
    private User findUser(String username){
        return userRepository.findById(username).orElseThrow();
    }

    /** Asserts that the stored row still holds the name, password hash, token and expiry of {@code original}. */
    private void assertUnchanged(User original){
        User userDb = findUser(original.getUsername());

        assertEquals(original.getName(), userDb.getName());
        assertEquals(original.getPassword(), userDb.getPassword());
        assertEquals(original.getToken(), userDb.getToken());
        assertEquals(original.getTokenExpiredAt(), userDb.getTokenExpiredAt());
    }

    private static Stream<Arguments> invalidRegisterRequests() {
        String over100Chars = "a".repeat(101);
        String over72Bytes = "🚀".repeat(19); // 19 x 4 bytes = 76 bytes

        return Stream.of(
                Arguments.of("username is empty",
                        new RegisterUserRequest(null, "Rahasia123", "Jhon Doe"),
                        "username",
                        "must not be blank"),
                Arguments.of("username is longer than 100 characters",
                        new RegisterUserRequest(over100Chars, "Rahasia123", "Jhon Doe"),
                        "username",
                        "size must be between 0 and 100"),
                Arguments.of("username contains whitespace",
                        new RegisterUserRequest("Jhon Doe", "Rahasia123", "Jhon Doe"),
                        "username",
                        "must not contain whitespace"),
                Arguments.of("password is empty",
                        new RegisterUserRequest("JhonDoe", "", "Jhon Doe"),
                        "password",
                        "must not be blank"),
                Arguments.of("password is longer than 72 bytes (ASCII)",
                        new RegisterUserRequest("JhonDoe", over100Chars, "Jhon Doe"),
                        "password",
                        "must not exceed 72 bytes"),
                Arguments.of("password is longer than 72 bytes (multi-byte)",
                        new RegisterUserRequest("JhonDoe", over72Bytes, "Jhon Doe"),
                        "password",
                        "must not exceed 72 bytes"),
                Arguments.of("name is empty",
                        new RegisterUserRequest("JhonDoe", "Rahasia123", ""),
                        "name",
                        "must not be blank"),
                Arguments.of("name is longer than 100 characters",
                        new RegisterUserRequest("JhonDoe", "Rahasia123", over100Chars),
                        "name",
                        "size must be between 0 and 100")
        );
    }

    private RegisterUserRequest validRegisterRequest(){
        return RegisterUserRequest.builder()
                .username(USERNAME)
                .password(RAW_PASSWORD)
                .name(NAME)
                .build();
    }

    private RestTestClient.ResponseSpec post(Object body){
        return client.post()
                .uri("/api/users")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }

    /** Calls {@code GET /api/users/current}. A {@code null} token omits the header entirely. */
    private RestTestClient.ResponseSpec getCurrent(String token){
        return client.get()
                .uri("/api/users/current")
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (token != null) {
                        headers.set(HEADER, token);
                    }
                })
                .exchange();
    }

    /** Calls {@code PATCH /api/users/current}. A {@code null} token omits the header entirely. */
    private RestTestClient.ResponseSpec patchCurrent(String token, Object body){
        return client.patch()
                .uri("/api/users/current")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (token != null) {
                        headers.set(HEADER, token);
                    }
                })
                .body(body)
                .exchange();
    }

    /** Sends a PATCH that must succeed and returns its response, already checked for {@code data} and no {@code errors}. */
    private WebResponse<UserResponse> patchOk(String token, Object body){
        WebResponse<UserResponse> response = patchCurrent(token, body)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getErrors());
        assertNotNull(response.getData());

        return response;
    }

    /** Calls {@code POST /api/auth/login} and returns the response for the caller to assert on. */
    private RestTestClient.ResponseSpec loginSpec(String username, String password){
        return client.post()
                .uri("/api/auth/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "password", password))
                .exchange();
    }

    /** Logs in through the real endpoint and returns the issued token. Uses a {@code Map} response so the test does not depend on the login DTOs. */
    private String login(String username, String password){
        WebResponse<Map<String, Object>> response = loginSpec(username, password)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<Map<String, Object>>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNotNull(response.getData());

        return (String) response.getData().get("token");
    }

    /** Asserts a {@code 401 UNAUTHORIZED} response with the standard error body and no validation details. */
    private void assertUnauthorized(RestTestClient.ResponseSpec spec){
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
        assertNull(response.getErrors().getDetails());
    }
}