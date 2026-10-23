package contactmanagement.controller;

import contactmanagement.entity.User;
import contactmanagement.model.RegisterUserRequest;
import contactmanagement.model.UserResponse;
import contactmanagement.model.WebResponse;
import contactmanagement.repository.UserRepository;
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

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureRestTestClient
class UserControllerTest {

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

    @Test
    void testRegisterResponseDoesNotExposePassword() {
        String rawBody = client.post()
                .uri("/api/users")
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

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void testRegisterInvalid(RegisterUserRequest request) {
        WebResponse<UserResponse> response = postRegisterBadRequest(request);

        assertNotNull(response);
        assertNotNull(response.getErrors());
        assertEquals(0, userRepository.count());
    }

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

    private static RegisterUserRequest request(String username, String password, String name) {
        return RegisterUserRequest.builder()
                .username(username)
                .password(password)
                .name(name)
                .build();
    }

    private WebResponse<UserResponse> postRegisterOK(RegisterUserRequest request) {
        return client.post()
                .uri("/api/users")
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

    private WebResponse<UserResponse> postRegisterBadRequest(RegisterUserRequest request) {
        return client.post()
                .uri("/api/users")
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