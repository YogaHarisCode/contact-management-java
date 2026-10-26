package contactmanagement.controller;

import contactmanagement.entity.User;
import contactmanagement.model.LoginResponse;
import contactmanagement.model.LoginUserRequest;
import contactmanagement.model.RegisterUserRequest;
import contactmanagement.model.UpdateUserRequest;
import contactmanagement.model.UserResponse;
import contactmanagement.model.WebResponse;
import contactmanagement.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link UserController} covering the register-user, get-current-user,
 * and update-current-user features.
 *
 * <p>Runs against the real datasource configured in {@code application.properties} and clears
 * the {@code users} table before and after every test, so point it to a dedicated test database.
 */
@SpringBootTest
@AutoConfigureRestTestClient
class UserControllerTest {

    private static final String USERS_URI = "/api/users";
    private static final String CURRENT_USER_URI = "/api/users/current";
    private static final String LOGIN_URI = "/api/auth/login";
    private static final String TOKEN_HEADER = "X-API-TOKEN";
    private static final String VALID_TOKEN = "valid-token-user-a";
    private static final String OTHER_TOKEN = "valid-token-user-b";

    @Autowired
    private RestTestClient client;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    // ============================================================ REGISTER

    @Nested
    class Register {

        /** A valid request returns 200 and stores a BCrypt hash instead of the raw password. */
        @Test
        void testRegisterSuccess() {
            RegisterUserRequest request = request("test", "test1234", "test");

            WebResponse<UserResponse> response = postRegisterOK(request);

            assertNotNull(response);
            assertNull(response.getErrors());
            assertEquals(request.getUsername(), response.getData().getUsername());
            assertEquals(request.getName(), response.getData().getName());

            User saved = userRepository.findById("test").orElseThrow();
            assertNotEquals(request.getPassword(), saved.getPassword());
            assertTrue(passwordEncoder.matches(request.getPassword(), saved.getPassword()));
        }

        /** The raw JSON response must contain neither a password field nor the password value. */
        @Test
        void testRegisterResponseDoesNotExposePassword() {
            String rawBody = client.post()
                    .uri(USERS_URI)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request("test", "test1234", "test"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(rawBody);
            assertFalse(rawBody.contains("password"));
            assertFalse(rawBody.contains("test1234"));
        }

        /** A duplicate username returns 400 and must not overwrite the existing account. */
        @Test
        void testRegisterDuplicateUsername() {
            User existing = new User();
            existing.setUsername("test");
            existing.setPassword("original-hash");
            existing.setName("original");
            userRepository.save(existing);

            WebResponse<UserResponse> response =
                    postRegisterBadRequest(request("test", "test1234", "other"));

            assertNotNull(response);
            assertEquals("Username already exists", response.getErrors());

            // Regression: existing account must not be overwritten.
            User unchanged = userRepository.findById("test").orElseThrow();
            assertEquals("original-hash", unchanged.getPassword());
            assertEquals("original", unchanged.getName());
        }

        /** Every invalid payload returns 400 with an error message and stores nothing. */
        @ParameterizedTest
        @MethodSource("invalidRequests")
        void testRegisterInvalid(RegisterUserRequest request) {
            WebResponse<UserResponse> response = postRegisterBadRequest(request);

            assertNotNull(response);
            assertNotNull(response.getErrors());
            assertEquals(0, userRepository.count());
        }

        /** Supplies one invalid payload per constraint: blank, null, and too long/short values. */
        static Stream<RegisterUserRequest> invalidRequests() {
            String tooLong = "a".repeat(101);
            return Stream.of(
                    request("", "test1234", "test"),
                    request(null, "test1234", "test"),
                    request(tooLong, "test1234", "test"),
                    request("test", "", "test"),
                    request("test", null, "test"),
                    request("test", "short", "test"),
                    request("test", tooLong, "test"),
                    request("test", "test1234", ""),
                    request("test", "test1234", null),
                    request("test", "test1234", tooLong)
            );
        }

        /** Shortcut for building a registration request. */
        private static RegisterUserRequest request(String username, String password, String name) {
            return RegisterUserRequest.builder()
                    .username(username)
                    .password(password)
                    .name(name)
                    .build();
        }

        /** Posts to {@code /api/users}, asserts HTTP 200, and returns the parsed response. */
        private WebResponse<UserResponse> postRegisterOK(RegisterUserRequest request) {
            return client.post()
                    .uri(USERS_URI)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();
        }

        /** Posts to {@code /api/users}, asserts HTTP 400, and returns the parsed response. */
        private WebResponse<UserResponse> postRegisterBadRequest(RegisterUserRequest request) {
            return client.post()
                    .uri(USERS_URI)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();
        }
    }

    // ======================================================= GET CURRENT USER

    @Nested
    class GetCurrentUser {

        @Test
        void shouldReturnCurrentUserProfileWhenTokenIsValid() {
            saveUser("usera", "User A", VALID_TOKEN, futureExpiry());

            WebResponse<UserResponse> response = getCurrentUser(VALID_TOKEN)
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertNotNull(response);
            assertNull(response.getErrors());
            assertEquals("usera", response.getData().getUsername());
            assertEquals("User A", response.getData().getName());
        }

        @Test
        void shouldReturnOwnProfileWhenMultipleUsersHaveTokens() {
            saveUser("usera", "User A", VALID_TOKEN, futureExpiry());
            saveUser("userb", "User B", OTHER_TOKEN, futureExpiry());

            WebResponse<UserResponse> response = getCurrentUser(OTHER_TOKEN)
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertNotNull(response);
            assertEquals("userb", response.getData().getUsername());
            assertEquals("User B", response.getData().getName());
        }

        @Test
        void shouldNotExposePasswordOrTokenWhenTokenIsValid() {
            saveUser("usera", "User A", VALID_TOKEN, futureExpiry());

            String rawBody = getCurrentUser(VALID_TOKEN)
                    .expectStatus().isOk()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(rawBody);
            assertFalse(rawBody.contains("password"));
            assertFalse(rawBody.contains("bcrypt-hash"));
            assertFalse(rawBody.contains("token"));
            assertFalse(rawBody.contains(VALID_TOKEN));
        }

        @Test
        void shouldReturnUnauthorizedWhenTokenHeaderIsMissing() {
            saveUser("usera", "User A", VALID_TOKEN, futureExpiry());

            assertUnauthorized(getCurrentUser(null));
        }

        @Test
        void shouldReturnUnauthorizedWhenTokenHeaderIsEmpty() {
            saveUser("usera", "User A", VALID_TOKEN, futureExpiry());

            assertUnauthorized(getCurrentUser(""));
        }

        @Test
        void shouldReturnUnauthorizedWhenTokenIsNotFound() {
            saveUser("usera", "User A", VALID_TOKEN, futureExpiry());

            assertUnauthorized(getCurrentUser("unknown-token"));
        }

        @Test
        void shouldReturnUnauthorizedWhenTokenIsExpired() {
            saveUser("usera", "User A", VALID_TOKEN, System.currentTimeMillis() - 1_000L);

            assertUnauthorized(getCurrentUser(VALID_TOKEN));
        }
    }

    // ===================================================== UPDATE CURRENT USER

    @Nested
    class UpdateCurrentUser {

        private static final String USERNAME = "johndoe";
        private static final String ORIGINAL_NAME = "John Doe";
        private static final String ORIGINAL_PASSWORD = "original123";

        private static final String OTHER_USERNAME = "janedoe";
        private static final String OTHER_NAME = "Jane Doe";

        private static final String NEW_NAME = "John Updated";
        private static final String NEW_PASSWORD = "newpassword123";

        /** Runs after the outer {@code setUp()}, so the table is already empty here. */
        @BeforeEach
        void setUpUser() {
            saveUserWithPassword(USERNAME, ORIGINAL_NAME, ORIGINAL_PASSWORD, VALID_TOKEN, futureExpiry());
        }

        // ------------------------------------------------------------ success

        @Test
        void shouldUpdateNameAndKeepPasswordWhenOnlyNameIsProvided() {
            WebResponse<UserResponse> response = patchOk(VALID_TOKEN,
                    UpdateUserRequest.builder().name(NEW_NAME).build());

            assertNotNull(response);
            assertNull(response.getErrors());
            assertEquals(USERNAME, response.getData().getUsername());
            assertEquals(NEW_NAME, response.getData().getName());

            User saved = userRepository.findById(USERNAME).orElseThrow();
            assertEquals(NEW_NAME, saved.getName());
            assertTrue(passwordEncoder.matches(ORIGINAL_PASSWORD, saved.getPassword()));
        }

        @Test
        void shouldUpdatePasswordAndKeepNameWhenOnlyPasswordIsProvided() {
            WebResponse<UserResponse> response = patchOk(VALID_TOKEN,
                    UpdateUserRequest.builder().password(NEW_PASSWORD).build());

            assertNotNull(response);
            assertEquals(ORIGINAL_NAME, response.getData().getName());

            User saved = userRepository.findById(USERNAME).orElseThrow();
            assertEquals(ORIGINAL_NAME, saved.getName());
            assertNotEquals(NEW_PASSWORD, saved.getPassword());
            assertTrue(passwordEncoder.matches(NEW_PASSWORD, saved.getPassword()));
            assertFalse(passwordEncoder.matches(ORIGINAL_PASSWORD, saved.getPassword()));
        }

        @Test
        void shouldUpdateNameAndPasswordWhenBothAreProvided() {
            patchOk(VALID_TOKEN, UpdateUserRequest.builder()
                    .name(NEW_NAME)
                    .password(NEW_PASSWORD)
                    .build());

            User saved = userRepository.findById(USERNAME).orElseThrow();
            assertEquals(NEW_NAME, saved.getName());
            assertTrue(passwordEncoder.matches(NEW_PASSWORD, saved.getPassword()));
        }

        @Test
        void shouldKeepAllValuesWhenBodyIsEmptyJsonObject() {
            WebResponse<UserResponse> response = patchOk(VALID_TOKEN, "{}");

            assertNotNull(response);
            assertEquals(ORIGINAL_NAME, response.getData().getName());
            assertUserUnchanged();
        }

        @Test
        void shouldAcceptValuesOnLengthBoundaries() {
            String nameOf100 = "a".repeat(100);

            patchOk(VALID_TOKEN, UpdateUserRequest.builder()
                    .name(nameOf100)
                    .password("12345678")
                    .build());

            User saved = userRepository.findById(USERNAME).orElseThrow();
            assertEquals(nameOf100, saved.getName());
            assertTrue(passwordEncoder.matches("12345678", saved.getPassword()));
        }

        @Test
        void shouldNotChangeTokenWhenProfileIsUpdated() {
            patchOk(VALID_TOKEN, UpdateUserRequest.builder()
                    .name(NEW_NAME)
                    .password(NEW_PASSWORD)
                    .build());

            User saved = userRepository.findById(USERNAME).orElseThrow();
            assertEquals(VALID_TOKEN, saved.getToken());
            assertNotNull(saved.getTokenExpiredAt());
        }

        @Test
        void shouldAllowLoginWithNewPasswordAndRejectOldOneWhenPasswordIsUpdated() {
            patchOk(VALID_TOKEN, UpdateUserRequest.builder().password(NEW_PASSWORD).build());

            WebResponse<LoginResponse> login = postLogin(USERNAME, NEW_PASSWORD)
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<WebResponse<LoginResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertNotNull(login);
            assertNotNull(login.getData().getToken());
            postLogin(USERNAME, ORIGINAL_PASSWORD).expectStatus().isUnauthorized();
        }

        @Test
        void shouldReflectUpdatedNameWhenCurrentUserIsFetchedAfterUpdate() {
            patchOk(VALID_TOKEN, UpdateUserRequest.builder().name(NEW_NAME).build());

            WebResponse<UserResponse> response = getCurrentUser(VALID_TOKEN)
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertNotNull(response);
            assertEquals(NEW_NAME, response.getData().getName());
        }

        @Test
        void shouldNotExposePasswordOrTokenWhenProfileIsUpdated() {
            String rawBody = patchCurrentUser(VALID_TOKEN, UpdateUserRequest.builder()
                    .name(NEW_NAME)
                    .password(NEW_PASSWORD)
                    .build())
                    .expectStatus().isOk()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(rawBody);
            assertFalse(rawBody.contains("password"));
            assertFalse(rawBody.contains(NEW_PASSWORD));
            assertFalse(rawBody.contains("token"));
            assertFalse(rawBody.contains(VALID_TOKEN));
        }

        @Test
        void shouldUpdateOnlyOwnAccountWhenMultipleUsersExist() {
            saveUserWithPassword(OTHER_USERNAME, OTHER_NAME, ORIGINAL_PASSWORD, OTHER_TOKEN, futureExpiry());

            patchOk(OTHER_TOKEN, UpdateUserRequest.builder().name(NEW_NAME).build());

            assertEquals(NEW_NAME, userRepository.findById(OTHER_USERNAME).orElseThrow().getName());
            assertUserUnchanged();
        }

        // ------------------------------------------------------- unauthorized

        @Test
        void shouldReturnUnauthorizedAndKeepDataWhenTokenHeaderIsMissing() {
            assertUnauthorized(patchCurrentUser(null, changeRequest()));

            assertUserUnchanged();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "unknown-token"})
        void shouldReturnUnauthorizedAndKeepDataWhenTokenIsEmptyOrUnknown(String token) {
            assertUnauthorized(patchCurrentUser(token, changeRequest()));

            assertUserUnchanged();
        }

        @Test
        void shouldReturnUnauthorizedAndKeepDataWhenTokenIsExpired() {
            User user = userRepository.findById(USERNAME).orElseThrow();
            user.setTokenExpiredAt(System.currentTimeMillis() - 1_000L);
            userRepository.save(user);

            assertUnauthorized(patchCurrentUser(VALID_TOKEN, changeRequest()));

            assertUserUnchanged();
        }

        /** The {@code User} parameter is resolved before the body, so 401 wins over 400. */
        @Test
        void shouldReturnUnauthorizedWhenTokenIsMissingAndBodyIsInvalid() {
            assertUnauthorized(patchCurrentUser(null, UpdateUserRequest.builder().password("short").build()));

            assertUserUnchanged();
        }

        // --------------------------------------------------------- validation

        @ParameterizedTest
        @MethodSource("invalidRequests")
        void shouldReturnBadRequestAndKeepDataWhenRequestFailsValidation(UpdateUserRequest invalid) {
            WebResponse<UserResponse> response = patchCurrentUser(VALID_TOKEN, invalid)
                    .expectStatus().isBadRequest()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertNotNull(response);
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertUserUnchanged();
        }

        @Test
        void shouldReturnBadRequestAndKeepDataWhenBodyIsMalformedJson() {
            WebResponse<UserResponse> response = patchCurrentUser(VALID_TOKEN, "{ \"name\": ")
                    .expectStatus().isBadRequest()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertNotNull(response);
            assertEquals("Malformed or missing request body", response.getErrors());
            assertUserUnchanged();
        }

        /** One invalid payload per constraint: blank, whitespace-only, too short, and too long. */
        static Stream<UpdateUserRequest> invalidRequests() {
            String tooLong = "a".repeat(101);
            return Stream.of(
                    UpdateUserRequest.builder().name("").build(),
                    UpdateUserRequest.builder().name("   ").build(),
                    UpdateUserRequest.builder().name(tooLong).build(),
                    UpdateUserRequest.builder().password("").build(),
                    UpdateUserRequest.builder().password("short").build(),
                    UpdateUserRequest.builder().password("        ").build(),
                    UpdateUserRequest.builder().password(tooLong).build()
            );
        }

        // ------------------------------------------------------------ helpers

        /** Sends PATCH, asserts HTTP 200, and returns the parsed response. */
        private WebResponse<UserResponse> patchOk(String token, Object body) {
            return patchCurrentUser(token, body)
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();
        }

        private RestTestClient.ResponseSpec postLogin(String username, String password) {
            return client.post()
                    .uri(LOGIN_URI)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(LoginUserRequest.builder().username(username).password(password).build())
                    .exchange();
        }

        /** Verifies the primary test user still has its original name and password. */
        private void assertUserUnchanged() {
            User user = userRepository.findById(USERNAME).orElseThrow();
            assertEquals(ORIGINAL_NAME, user.getName());
            assertTrue(passwordEncoder.matches(ORIGINAL_PASSWORD, user.getPassword()));
        }

        private static UpdateUserRequest changeRequest() {
            return UpdateUserRequest.builder()
                    .name(NEW_NAME)
                    .password(NEW_PASSWORD)
                    .build();
        }
    }

    // ============================================================== SHARED

    /** Sends GET /api/users/current; the token header is omitted when {@code token} is null. */
    private RestTestClient.ResponseSpec getCurrentUser(String token) {
        return client.get()
                .uri(CURRENT_USER_URI)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (token != null) {
                        headers.add(TOKEN_HEADER, token);
                    }
                })
                .exchange();
    }

    /** Sends PATCH /api/users/current; the token header is omitted when {@code token} is null. */
    private RestTestClient.ResponseSpec patchCurrentUser(String token, Object body) {
        return client.patch()
                .uri(CURRENT_USER_URI)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (token != null) {
                        headers.add(TOKEN_HEADER, token);
                    }
                })
                .body(body)
                .exchange();
    }

    private void assertUnauthorized(RestTestClient.ResponseSpec spec) {
        WebResponse<UserResponse> response = spec
                .expectStatus().isUnauthorized()
                .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());
        assertEquals("Unauthorized", response.getErrors());
    }

    /** Saves a user with the literal hash {@code "bcrypt-hash"} (no BCrypt cost). */
    private void saveUser(String username, String name, String token, long expiredAt) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("bcrypt-hash");
        user.setName(name);
        user.setToken(token);
        user.setTokenExpiredAt(expiredAt);
        userRepository.save(user);
    }

    /** Saves a user whose password is the real BCrypt hash of {@code rawPassword}. */
    private void saveUserWithPassword(String username, String name, String rawPassword,
                                      String token, long expiredAt) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setName(name);
        user.setToken(token);
        user.setTokenExpiredAt(expiredAt);
        userRepository.save(user);
    }

    private static long futureExpiry() {
        return System.currentTimeMillis() + 60_000L;
    }
}