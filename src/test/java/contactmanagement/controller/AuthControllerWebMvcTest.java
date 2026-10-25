package contactmanagement.controller;

import contactmanagement.model.LoginResponse;
import contactmanagement.model.LoginUserRequest;
import contactmanagement.model.WebResponse;
import contactmanagement.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(AuthController.class)
@AutoConfigureRestTestClient
class AuthControllerWebMvcTest {

    private static final String LOGIN_URI = "/api/auth/login";

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private AuthService authService;

    @Test
    void shouldReturnTokenWhenLoginRequestIsValid() {
        LoginResponse loginResponse = LoginResponse.builder()
                .token("3c829e12-4217-48f5-a169-715a31bc0d6d")
                .tokenExpiredAt(1_900_000_000_000L)
                .build();
        when(authService.login(any(LoginUserRequest.class))).thenReturn(loginResponse);

        WebResponse<LoginResponse> response = postLogin(request("test", "test1234"))
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<LoginResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getErrors());
        assertEquals("3c829e12-4217-48f5-a169-715a31bc0d6d", response.getData().getToken());
        assertEquals(1_900_000_000_000L, response.getData().getTokenExpiredAt());
    }

    @Test
    void shouldPassRequestFieldsToServiceWhenLoginRequestIsValid() {
        when(authService.login(any(LoginUserRequest.class)))
                .thenReturn(LoginResponse.builder().token("token").tokenExpiredAt(1L).build());

        postLogin(request("test", "test1234")).expectStatus().isOk();

        verify(authService).login(request("test", "test1234"));
    }

    @Test
    void shouldNotExposePasswordFieldInSuccessResponse() {
        when(authService.login(any(LoginUserRequest.class)))
                .thenReturn(LoginResponse.builder().token("token").tokenExpiredAt(1L).build());

        String rawBody = postLogin(request("test", "test1234"))
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(rawBody);
        assertTrue(!rawBody.contains("password") && !rawBody.contains("test1234"));
    }

    @Test
    void shouldReturnUnauthorizedWhenServiceRejectsCredentials() {
        when(authService.login(any(LoginUserRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or password wrong"));

        WebResponse<LoginResponse> response = postLogin(request("test", "wrong-password"))
                .expectStatus().isUnauthorized()
                .expectBody(new ParameterizedTypeReference<WebResponse<LoginResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());
        assertEquals("Username or password wrong", response.getErrors());
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void shouldReturnBadRequestWhenRequestFailsValidation(LoginUserRequest invalidRequest) {
        WebResponse<LoginResponse> response = postLogin(invalidRequest)
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<LoginResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());
        assertNotNull(response.getErrors());
        verifyNoInteractions(authService);
    }

    @Test
    void shouldMentionFieldNameInErrorWhenPasswordIsBlank() {
        WebResponse<LoginResponse> response = postLogin(request("test", ""))
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<LoginResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertTrue(response.getErrors().toString().contains("password"));
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsMalformedJson() {
        WebResponse<LoginResponse> response = postLogin("{ \"username\": \"test\", ")
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<LoginResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals("Malformed or missing request body", response.getErrors());
        verifyNoInteractions(authService);
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsMissing() {
        WebResponse<LoginResponse> response = client.post()
                .uri(LOGIN_URI)
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
        verifyNoInteractions(authService);
    }

    /** One invalid payload per constraint: blank, null, and over-length for each field. */
    static Stream<LoginUserRequest> invalidRequests() {
        String tooLong = "a".repeat(101);
        return Stream.of(
                request("", "test1234"),
                request(null, "test1234"),
                request("   ", "test1234"),
                request(tooLong, "test1234"),
                request("test", ""),
                request("test", null),
                request("test", "   "),
                request("test", tooLong)
        );
    }

    private static LoginUserRequest request(String username, String password) {
        return LoginUserRequest.builder()
                .username(username)
                .password(password)
                .build();
    }

    private RestTestClient.ResponseSpec postLogin(Object body) {
        return client.post()
                .uri(LOGIN_URI)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }
}