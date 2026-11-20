package com.yogaharis.contactmanagement.controller;

import com.yogaharis.contactmanagement.dto.RegisterUserRequest;
import com.yogaharis.contactmanagement.dto.UpdateUserRequest;
import com.yogaharis.contactmanagement.dto.UserResponse;
import com.yogaharis.contactmanagement.dto.WebResponse;
import com.yogaharis.contactmanagement.entity.User;
import com.yogaharis.contactmanagement.repository.UserRepository;
import com.yogaharis.contactmanagement.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Controller slice tests for {@link UserController} using {@code @WebMvcTest}.
 * The service layer is mocked; validation and error responses are verified over HTTP.
 *
 * <p>Get current user is tested through the real {@code UserArgumentResolver}, which looks the
 * user up by token in the mocked {@link UserRepository}.</p>
 *
 * @author Yoga Harisistianugroho
 */
@WebMvcTest(UserController.class)
@AutoConfigureRestTestClient
public class UserControllerWebMvcTest {

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private UserService userService;

    // Needed because the slice also loads UserArgumentResolver, which finds users by token.
    @MockitoBean
    private UserRepository userRepository;

    private static final String USERNAME = "JhonDoe";
    private static final String RAW_PASSWORD = "Rahasia123";
    private static final String NAME = "Jhon Doe";
    private static final String VALID_TOKEN = "TOKEN-VALID";
    private static final String INVALID_TOKEN = "Invalid-token";
    private static final String HEADER = "X-API-TOKEN";
    private static final String HASH_PASSWORD = "Hash-password";

    // ---------------------------------------------------------- REGISTER

    @Test
    void testRegisterSuccess() {
        // arrange
        RegisterUserRequest request = validRegisterRequest();

        when(userService.register(any(RegisterUserRequest.class)))
                .thenAnswer(invocation -> {
                    RegisterUserRequest request1 = invocation.getArgument(0);

                    return new UserResponse(request1.getUsername(), request1.getName());
                });

        // act
        WebResponse<UserResponse> response = post(request)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        // assert
        assertNotNull(response);
        assertEquals(USERNAME,response.getData().getUsername());
        assertEquals(NAME, response.getData().getName());
        assertNull(response.getErrors());

        // verify
        ArgumentCaptor<RegisterUserRequest> userArgumentCaptor = ArgumentCaptor.forClass(RegisterUserRequest.class);
        verify(userService).register(userArgumentCaptor.capture());
        assertEquals(request, userArgumentCaptor.getValue());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({"invalidRegisterRequests"})
    void testRegisterValidation(String scenario, RegisterUserRequest request, String invalidField, String invalidMessage) {
        WebResponse<Void> response = post(request)
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<Void>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);

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

        assertNull(response.getData());

        verify(userService, never())
                .register(any(RegisterUserRequest.class));
    }

    @Test
    void testRegisterDuplicateUsername() {
        RegisterUserRequest request = validRegisterRequest();

        when(userService.register(request))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists"));

        WebResponse<Void> response = post(request)
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(new ParameterizedTypeReference<WebResponse<Void>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);

        assertNotNull(response.getErrors());
        assertEquals(HttpStatus.CONFLICT.toString(), response.getErrors().getCode());
        assertEquals("Username already exists", response.getErrors().getMessage());

        assertNull(response.getData());

        verify(userService)
                .register(request);

    }

    // ---------------------------------------------------------- GET
    /** The resolver finds the user by token in the mocked repository and passes it to the service. */
    @Test
    void testGetCurrentUserSuccess() {
        User user = registeredUser();

        when(userRepository.findByToken(VALID_TOKEN))
                .thenReturn(Optional.of(user));

        when(userService.get(user))
                .thenReturn(UserResponse.builder().username(user.getUsername()).name(user.getName()).build());

        WebResponse<UserResponse> response = getCurrent(VALID_TOKEN)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNotNull(response.getData());

        assertEquals(USERNAME, response.getData().getUsername());
        assertEquals(NAME, response.getData().getName());

        assertNull(response.getErrors());

        verify(userRepository).findByToken(VALID_TOKEN);
        verify(userService).get(user);
    }

    /** Checks the raw JSON, since the deserialized DTO could not show extra or leaked fields. */
    @Test
    void testGetCurrentUserDoesNotExposeSensitiveFields() {
        User user = registeredUser();

        when(userRepository.findByToken(VALID_TOKEN))
                .thenReturn(Optional.of(user));

        when(userService.get(user))
                .thenReturn(UserResponse.builder().username(user.getUsername()).name(user.getName()).build());

        getCurrent(VALID_TOKEN)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.username").isEqualTo(USERNAME)
                .jsonPath("$.data.name").isEqualTo(NAME)
                .jsonPath("$.data.password").doesNotExist()
                .jsonPath("$.data.token").doesNotExist()
                .jsonPath("$.data.tokenExpiredAt").doesNotExist()
                .jsonPath("$.errors").doesNotExist();
    }

    @Test
    void testGetCurrentUserMissingToken() {
        assertUnauthorized(getCurrent(null));

        verifyNoInteractions(userRepository, userService);
    }

    /** An empty or whitespace-only header is rejected like a missing one, without a database lookup. */
    @ParameterizedTest(name = "token = [{0}]")
    @ValueSource(strings = {"", "   "})
    void testGetCurrentUserBlankToken(String token) {
        assertUnauthorized(getCurrent(token));

        verifyNoInteractions(userRepository, userService);
    }

    @Test
    void testGetCurrentUserInvalidToken() {
        when(userRepository.findByToken(INVALID_TOKEN))
                .thenReturn(Optional.empty());

        assertUnauthorized(getCurrent(INVALID_TOKEN));

        verify(userRepository).findByToken(INVALID_TOKEN);
        verify(userService, never()).get(any(User.class));
    }

    @Test
    void testGetCurrentUserExpiredToken() {
        User user = registeredUser(Instant.now().minus(Duration.ofSeconds(1)).toEpochMilli());

        when(userRepository.findByToken(VALID_TOKEN))
                .thenReturn(Optional.of(user));

        assertUnauthorized(getCurrent(VALID_TOKEN));

        verify(userRepository).findByToken(VALID_TOKEN);
        verify(userService, never()).get(any(User.class));
    }

    /** A token without an expiry time is treated as invalid. */
    @Test
    void testGetCurrentUserNullTokenExpiredAt() {
        User user = registeredUser(null);

        when(userRepository.findByToken(VALID_TOKEN))
                .thenReturn(Optional.of(user));

        assertUnauthorized(getCurrent(VALID_TOKEN));

        verify(userRepository).findByToken(VALID_TOKEN);
        verify(userService, never()).get(any(User.class));
    }

    /** {@code /api/users/current} only maps GET, so POST must be rejected before any lookup. */
    @Test
    void testGetCurrentUserMethodNotAllowed() {
        WebResponse<Void> response = client.post()
                .uri("/api/users/current")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
                .expectBody(new ParameterizedTypeReference<WebResponse<Void>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);

        assertNull(response.getData());

        assertNotNull(response.getErrors());
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.toString(), response.getErrors().getCode());

        verifyNoInteractions(userRepository, userService);
    }

    // ---------------------------------------------------------- UPDATE

    @Test
    void testUpdateUserSuccess() {
        // arrange
        User user = new User();
        user.setUsername("JohnDoe");
        user.setPassword("Hash-Password");
        user.setName("John Doe");
        user.setToken("Valid-Token");
        user.setTokenExpiredAt(Instant.now().plus(Duration.ofDays(1)).toEpochMilli());

        UpdateUserRequest request = UpdateUserRequest.builder()
                .name("John Doe Updated")
                .password("NewPassword")
                .build();

        when(userRepository.findByToken("Valid-Token"))
                .thenReturn(Optional.of(user));

        when(userService.update(user, request))
                .thenReturn(UserResponse.builder().username(user.getUsername()).name(request.getName()).build());

        // act
        WebResponse<UserResponse> response = client.patch()
                .uri("/api/users/current")
                .header("X-API-TOKEN", "Valid-Token")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        // assert
        assertNotNull(response);
        assertNull(response.getErrors());
        assertNotNull(response.getData());
        assertEquals(user.getUsername(), response.getData().getUsername());
        assertNotEquals(user.getName(), response.getData().getName());
        assertEquals(request.getName(), response.getData().getName());

        verify(userRepository).findByToken("Valid-Token");
        verify(userService).update(user, request);
    }

    @ParameterizedTest
    @MethodSource({"partialAndEmptyUpdateRequest"})
    void testUpdatePartialAndEmptyJsonSuccess(UpdateUserRequest request) {
        // arrange
        User user = new User();
        user.setUsername("JohnDoe");
        user.setPassword("Hash-Password");
        user.setName("John Doe");
        user.setToken("Valid-Token");
        user.setTokenExpiredAt(Instant.now().plus(Duration.ofDays(1)).toEpochMilli());

        when(userRepository.findByToken("Valid-Token"))
                .thenReturn(Optional.of(user));

        when(userService.update(user, request))
                .thenReturn(UserResponse.builder().username(user.getUsername()).name(request.getName()).build());

        // act
        WebResponse<UserResponse> response = client.patch()
                .uri("/api/users/current")
                .header("X-API-TOKEN", "Valid-Token")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        // assert
        assertNotNull(response);
        assertNull(response.getErrors());
        assertNotNull(response.getData());

        verify(userRepository).findByToken("Valid-Token");
        verify(userService).update(user, request);
    }

    @ParameterizedTest
    @MethodSource({"invalidUpdateRequest"})
    void testUpdateValidationFailed(UpdateUserRequest request) {
        // arrange
        User user = new User();
        user.setUsername("JohnDoe");
        user.setPassword("Hash-Password");
        user.setName("John Doe");
        user.setToken("Valid-Token");
        user.setTokenExpiredAt(Instant.now().plus(Duration.ofDays(1)).toEpochMilli());

        when(userRepository.findByToken("Valid-Token"))
                .thenReturn(Optional.of(user));

        // act
        WebResponse<UserResponse> response = client.patch()
                .uri("/api/users/current")
                .header("X-API-TOKEN", "Valid-Token")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
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

        verify(userRepository).findByToken("Valid-Token");
        verifyNoInteractions(userService);
    }

    @Test
    void testUpdateUnauthorized() {
        // arrange
        User user = new User();
        user.setUsername("JohnDoe");
        user.setPassword("Hash-Password");
        user.setName("John Doe");
        user.setToken("Valid-Token");
        user.setTokenExpiredAt(Instant.now().plus(Duration.ofDays(1)).toEpochMilli());

        UpdateUserRequest request = UpdateUserRequest.builder()
                .name("John Doe Updated")
                .password("NewPassword")
                .build();

        when(userRepository.findByToken("Valid-Token"))
                .thenReturn(Optional.empty());

        // act
        WebResponse<UserResponse> response = client.patch()
                .uri("/api/users/current")
                .header("X-API-TOKEN", "Valid-Token")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        // assert
        assertNotNull(response);
        assertNull(response.getData());
        assertNotNull(response.getErrors());
        assertEquals(HttpStatus.UNAUTHORIZED.toString(), response.getErrors().getCode());
        assertEquals("Unauthorized", response.getErrors().getMessage());
        assertNull(response.getErrors().getDetails());

        verify(userRepository).findByToken("Valid-Token");
        verifyNoInteractions(userService);
    }


    // ---------------------------------------------------------- HELPER

    private static Stream<Arguments> invalidUpdateRequest(){
        String over100 = "a".repeat(101);
        String over72Bytes = "🚀".repeat(19);

        return Stream.of(
                Arguments.of(
                        UpdateUserRequest.builder().name("").build()
                ),
                Arguments.of(
                        UpdateUserRequest.builder().name("   ").build()
                ),
                Arguments.of(
                        UpdateUserRequest.builder().name(over100).build()
                ),
                Arguments.of(
                        UpdateUserRequest.builder().password("").build()
                ),
                Arguments.of(
                        UpdateUserRequest.builder().password("   ").build()
                ),
                Arguments.of(
                        UpdateUserRequest.builder().password("123").build()
                ),
                Arguments.of(
                        UpdateUserRequest.builder().password(over100).build()
                ),
                Arguments.of(
                        UpdateUserRequest.builder().password(over72Bytes).build()
                )
        );
    }

    private static Stream<Arguments> partialAndEmptyUpdateRequest(){
        return Stream.of(
                Arguments.of(
                        UpdateUserRequest.builder().name("John Doe Updated").build()
                ),
                Arguments.of(
                        UpdateUserRequest.builder().password("NewPassword").build()
                ),
                Arguments.of(
                        UpdateUserRequest.builder().build()
                )
        );
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

    /** Builds an in-memory user (not persisted) holding {@code VALID_TOKEN}, valid for one hour. */
    private User registeredUser(){
        return registeredUser(Instant.now().plus(Duration.ofHours(1)).toEpochMilli());
    }

    /** Builds an in-memory user (not persisted) holding {@code VALID_TOKEN} with the given expiry. */
    private User registeredUser(Long tokenExpiredAt){
        User user = new User();
        user.setUsername(USERNAME);
        user.setPassword(HASH_PASSWORD);
        user.setName(NAME);
        user.setToken(VALID_TOKEN);
        user.setTokenExpiredAt(tokenExpiredAt);
        return user;
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
                        new RegisterUserRequest("JhonDoe", " ", "Jhon Doe"),
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
}