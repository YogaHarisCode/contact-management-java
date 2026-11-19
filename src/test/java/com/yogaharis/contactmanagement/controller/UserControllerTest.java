package com.yogaharis.contactmanagement.controller;

import com.yogaharis.contactmanagement.dto.RegisterUserRequest;
import com.yogaharis.contactmanagement.dto.UserResponse;
import com.yogaharis.contactmanagement.dto.WebResponse;
import com.yogaharis.contactmanagement.entity.User;
import com.yogaharis.contactmanagement.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
 * Integration tests for the user endpoints, running the full application context
 * against the {@code contact_management_test} database.
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

    private static final String USERNAME = "JhonDoe";
    private static final String RAW_PASSWORD = "Rahasia123";
    private static final String NAME = "Jhon Doe";
    private static final String VALID_TOKEN = "Valid-Token";
    private static final String INVALID_TOKEN = "Invalid-Token";
    private static final String HEADER = "X-API-TOKEN";

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
        assertTrue(
                response.getErrors().getDetails().stream()
                        .anyMatch(detail ->
                                detail.getField().equals(invalidField)
                                        && detail.getMessage().equals(invalidMessage)
                        )
        );
    }

    // ---------------------------------------------------------- GET

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

    @Test
    void testGetCurrentUserNullTokenExpiredAt() {
        registeredUser(USERNAME, NAME, VALID_TOKEN, null);

        assertUnauthorized(getCurrent(VALID_TOKEN));
    }

    // ---------------------------------------------------------- HELPER

    private User registeredUser(){
        return registeredUser(USERNAME, NAME, VALID_TOKEN, Instant.now().plus(Duration.ofDays(1)).toEpochMilli());
    }

    private User registeredUser(String username, String name, String token, Long tokenExpiredAt){
        User user = new User();
        user.setUsername(username);
        user.setPassword("hash-password");
        user.setName(name);
        user.setToken(token);
        user.setTokenExpiredAt(tokenExpiredAt);
        return userRepository.save(user);
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

    private String login(String username, String password){
        WebResponse<Map<String, Object>> response = client.post()
                .uri("/api/auth/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "password", password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<Map<String, Object>>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNotNull(response.getData());

        return (String) response.getData().get("token");
    }

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