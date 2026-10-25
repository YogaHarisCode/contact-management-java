package contactmanagement.controller;

import contactmanagement.entity.User;
import contactmanagement.model.UserResponse;
import contactmanagement.model.WebResponse;
import contactmanagement.repository.UserRepository;
import contactmanagement.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(UserController.class)
@AutoConfigureRestTestClient
class UserControllerGetUserWebMvcTest {

    private static final String CURRENT_USER_URI = "/api/users/current";
    private static final String TOKEN_HEADER = "X-API-TOKEN";
    private static final String TOKEN = "3c829e12-4217-48f5-a169-715a31bc0d6d";

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void shouldReturnUsernameAndNameWhenTokenIsValid() {
        User user = validUser();
        when(userRepository.findFirstByToken(TOKEN)).thenReturn(Optional.of(user));
        when(userService.get(user)).thenReturn(userResponse());

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
        User user = validUser();
        when(userRepository.findFirstByToken(TOKEN)).thenReturn(Optional.of(user));
        when(userService.get(user)).thenReturn(userResponse());

        getCurrentUser(TOKEN).expectStatus().isOk();

        verify(userService).get(user);
    }

    @Test
    void shouldNotExposePasswordOrTokenWhenTokenIsValid() {
        User user = validUser();
        when(userRepository.findFirstByToken(TOKEN)).thenReturn(Optional.of(user));
        when(userService.get(user)).thenReturn(userResponse());

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
        User expired = validUser();
        expired.setTokenExpiredAt(System.currentTimeMillis() - 1_000L);
        when(userRepository.findFirstByToken(TOKEN)).thenReturn(Optional.of(expired));

        assertUnauthorized(getCurrentUser(TOKEN));

        verifyNoInteractions(userService);
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

    private static User validUser() {
        User user = new User();
        user.setUsername("johndoe");
        user.setPassword("bcrypt-hash");
        user.setName("John Doe");
        user.setToken(TOKEN);
        user.setTokenExpiredAt(System.currentTimeMillis() + 60_000L);
        return user;
    }

    private static UserResponse userResponse() {
        return UserResponse.builder()
                .username("johndoe")
                .name("John Doe")
                .build();
    }
}