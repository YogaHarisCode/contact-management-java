package contactmanagement.controller;

import contactmanagement.entity.User;
import contactmanagement.model.UpdateUserRequest;
import contactmanagement.model.UserResponse;
import contactmanagement.model.WebResponse;
import contactmanagement.repository.UserRepository;
import contactmanagement.service.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Web-layer tests for {@code GET /api/users/current} and {@code PATCH /api/users/current}.
 *
 * <p>Runs the real {@link UserController}, argument resolver, and exception handler, with
 * {@link UserService} and {@link UserRepository} mocked, so no database is needed.
 */
@WebMvcTest(UserController.class)
@AutoConfigureRestTestClient
class UserControllerWebMvcTest {

    private static final String CURRENT_USER_URI = "/api/users/current";
    private static final String TOKEN_HEADER = "X-API-TOKEN";
    private static final String TOKEN = "3c829e12-4217-48f5-a169-715a31bc0d6d";

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    // ================================================================ GET

    @Nested
    class GetCurrentUser {

        @Test
        void shouldReturnUsernameAndNameWhenTokenIsValid() {
            User user = givenAuthenticatedUser();
            when(userService.get(user)).thenReturn(userResponse("John Doe"));

            WebResponse<UserResponse> response = getCurrentUser(TOKEN)
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertNotNull(response);
            assertNull(response.getErrors());
            assertEquals("johndoe", response.getData().getUsername());
            assertEquals("John Doe", response.getData().getName());
        }

        @Test
        void shouldPassAuthenticatedUserToServiceWhenTokenIsValid() {
            User user = givenAuthenticatedUser();
            when(userService.get(user)).thenReturn(userResponse("John Doe"));

            getCurrentUser(TOKEN).expectStatus().isOk();

            verify(userService).get(user);
        }

        @Test
        void shouldNotExposePasswordOrTokenWhenTokenIsValid() {
            User user = givenAuthenticatedUser();
            when(userService.get(user)).thenReturn(userResponse("John Doe"));

            String rawBody = getCurrentUser(TOKEN)
                    .expectStatus().isOk()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(rawBody);
            assertFalse(rawBody.contains("password"));
            assertFalse(rawBody.contains("bcrypt-hash"));
            assertFalse(rawBody.contains("token"));
            assertFalse(rawBody.contains(TOKEN));
        }

        @Test
        void shouldReturnUnauthorizedWhenTokenHeaderIsMissing() {
            assertUnauthorized(getCurrentUser(null));

            verifyNoInteractions(userRepository, userService);
        }

        @Test
        void shouldReturnUnauthorizedWhenTokenHeaderIsEmpty() {
            assertUnauthorized(getCurrentUser(""));

            verifyNoInteractions(userRepository, userService);
        }

        @Test
        void shouldReturnUnauthorizedWhenTokenIsNotFound() {
            when(userRepository.findFirstByToken("unknown-token")).thenReturn(Optional.empty());

            assertUnauthorized(getCurrentUser("unknown-token"));

            verifyNoInteractions(userService);
        }

        @Test
        void shouldReturnUnauthorizedWhenTokenIsExpired() {
            givenExpiredUser();

            assertUnauthorized(getCurrentUser(TOKEN));

            verifyNoInteractions(userService);
        }
    }

    // ============================================================== PATCH

    @Nested
    class UpdateCurrentUser {

        // ---------------------------------------------------------- success

        @Test
        void shouldReturnUpdatedProfileWhenRequestIsValid() {
            User user = givenAuthenticatedUser();
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .name("Jane Doe")
                    .password("newpassword123")
                    .build();
            when(userService.update(user, request)).thenReturn(userResponse("Jane Doe"));

            WebResponse<UserResponse> response = patchCurrentUser(TOKEN, request)
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertNotNull(response);
            assertNull(response.getErrors());
            assertEquals("johndoe", response.getData().getUsername());
            assertEquals("Jane Doe", response.getData().getName());
        }

        @Test
        void shouldPassAuthenticatedUserAndRequestToServiceWhenOnlyNameIsProvided() {
            User user = givenAuthenticatedUser();
            UpdateUserRequest expected = UpdateUserRequest.builder().name("Jane Doe").build();
            when(userService.update(user, expected)).thenReturn(userResponse("Jane Doe"));

            patchCurrentUser(TOKEN, expected).expectStatus().isOk();

            verify(userService).update(user, expected);
        }

        @Test
        void shouldPassAuthenticatedUserAndRequestToServiceWhenOnlyPasswordIsProvided() {
            User user = givenAuthenticatedUser();
            UpdateUserRequest expected = UpdateUserRequest.builder().password("newpassword123").build();
            when(userService.update(user, expected)).thenReturn(userResponse("John Doe"));

            patchCurrentUser(TOKEN, expected).expectStatus().isOk();

            verify(userService).update(user, expected);
        }

        @Test
        void shouldReturnOkWhenBodyIsEmptyJsonObject() {
            User user = givenAuthenticatedUser();
            UpdateUserRequest expected = new UpdateUserRequest();
            when(userService.update(user, expected)).thenReturn(userResponse("John Doe"));

            patchCurrentUser(TOKEN, "{}").expectStatus().isOk();

            verify(userService).update(user, expected);
        }

        @Test
        void shouldNotExposePasswordOrTokenWhenRequestIsValid() {
            User user = givenAuthenticatedUser();
            UpdateUserRequest request = UpdateUserRequest.builder()
                    .name("Jane Doe")
                    .password("newpassword123")
                    .build();
            when(userService.update(user, request)).thenReturn(userResponse("Jane Doe"));

            String rawBody = patchCurrentUser(TOKEN, request)
                    .expectStatus().isOk()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(rawBody);
            assertFalse(rawBody.contains("password"));
            assertFalse(rawBody.contains("newpassword123"));
            assertFalse(rawBody.contains("bcrypt-hash"));
            assertFalse(rawBody.contains("token"));
            assertFalse(rawBody.contains(TOKEN));
        }

        // ----------------------------------------------------- unauthorized

        @Test
        void shouldReturnUnauthorizedWhenTokenHeaderIsMissing() {
            assertUnauthorized(patchCurrentUser(null, validRequest()));

            verifyNoInteractions(userRepository, userService);
        }

        @Test
        void shouldReturnUnauthorizedWhenTokenHeaderIsEmpty() {
            assertUnauthorized(patchCurrentUser("", validRequest()));

            verifyNoInteractions(userRepository, userService);
        }

        @Test
        void shouldReturnUnauthorizedWhenTokenIsNotFound() {
            when(userRepository.findFirstByToken("unknown-token")).thenReturn(Optional.empty());

            assertUnauthorized(patchCurrentUser("unknown-token", validRequest()));

            verifyNoInteractions(userService);
        }

        @Test
        void shouldReturnUnauthorizedWhenTokenIsExpired() {
            givenExpiredUser();

            assertUnauthorized(patchCurrentUser(TOKEN, validRequest()));

            verifyNoInteractions(userService);
        }

        /** The {@code User} parameter is resolved before the body, so 401 wins over 400. */
        @Test
        void shouldReturnUnauthorizedWhenTokenIsMissingAndBodyIsInvalid() {
            UpdateUserRequest invalid = UpdateUserRequest.builder().password("short").build();

            assertUnauthorized(patchCurrentUser(null, invalid));

            verifyNoInteractions(userService);
        }

        // ------------------------------------------------------- validation

        @ParameterizedTest
        @MethodSource("invalidRequests")
        void shouldReturnBadRequestWhenRequestFailsValidation(UpdateUserRequest invalid) {
            givenAuthenticatedUser();

            WebResponse<UserResponse> response = patchCurrentUser(TOKEN, invalid)
                    .expectStatus().isBadRequest()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertNotNull(response);
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            verifyNoInteractions(userService);
        }

        @Test
        void shouldMentionFieldNameInErrorWhenPasswordIsTooShort() {
            givenAuthenticatedUser();

            WebResponse<UserResponse> response = patchCurrentUser(TOKEN,
                    UpdateUserRequest.builder().password("short").build())
                    .expectStatus().isBadRequest()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertNotNull(response);
            assertTrue(response.getErrors().toString().contains("password"));
        }

        @Test
        void shouldMentionFieldNameInErrorWhenNameIsBlank() {
            givenAuthenticatedUser();

            WebResponse<UserResponse> response = patchCurrentUser(TOKEN,
                    UpdateUserRequest.builder().name("   ").build())
                    .expectStatus().isBadRequest()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertNotNull(response);
            String errors = response.getErrors().toString();
            assertTrue(errors.contains("name"));
            assertTrue(errors.contains("must not be blank"));
        }

        @Test
        void shouldNotEchoRejectedPasswordInErrorWhenPasswordIsInvalid() {
            givenAuthenticatedUser();

            WebResponse<UserResponse> response = patchCurrentUser(TOKEN,
                    UpdateUserRequest.builder().password("pass-7c").build())
                    .expectStatus().isBadRequest()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertNotNull(response);
            assertFalse(response.getErrors().toString().contains("pass-7c"));
        }

        @Test
        void shouldReturnBadRequestWhenBodyIsMalformedJson() {
            givenAuthenticatedUser();

            WebResponse<UserResponse> response = patchCurrentUser(TOKEN, "{ \"name\": ")
                    .expectStatus().isBadRequest()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertNotNull(response);
            assertEquals("Malformed or missing request body", response.getErrors());
            verifyNoInteractions(userService);
        }

        @Test
        void shouldReturnBadRequestWhenBodyIsMissing() {
            givenAuthenticatedUser();

            WebResponse<UserResponse> response = client.patch()
                    .uri(CURRENT_USER_URI)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.add(TOKEN_HEADER, TOKEN))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody(new ParameterizedTypeReference<WebResponse<UserResponse>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertNotNull(response);
            assertEquals("Malformed or missing request body", response.getErrors());
            verifyNoInteractions(userService);
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
    }

    // ============================================================ helpers

    /** Stubs the repository so the resolver authenticates {@link #TOKEN} as {@code johndoe}. */
    private User givenAuthenticatedUser() {
        User user = validUser();
        when(userRepository.findFirstByToken(TOKEN)).thenReturn(Optional.of(user));
        return user;
    }

    /** Stubs the repository so {@link #TOKEN} belongs to a user whose token has expired. */
    private void givenExpiredUser() {
        User expired = validUser();
        expired.setTokenExpiredAt(System.currentTimeMillis() - 1_000L);
        when(userRepository.findFirstByToken(TOKEN)).thenReturn(Optional.of(expired));
    }

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

    private static UpdateUserRequest validRequest() {
        return UpdateUserRequest.builder().name("Jane Doe").build();
    }

    private static User validUser() {
        User user = new User();
        user.setUsername("johndoe");
        user.setPassword("bcrypt-hash");
        user.setName("John Doe");
        user.setToken(TOKEN);
        user.setTokenExpiredAt(System.currentTimeMillis() + 60_000L);
        return user;
    }

    private static UserResponse userResponse(String name) {
        return UserResponse.builder()
                .username("johndoe")
                .name(name)
                .build();
    }
}