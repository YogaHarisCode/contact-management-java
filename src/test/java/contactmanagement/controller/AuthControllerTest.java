package contactmanagement.controller;

import contactmanagement.entity.User;
import contactmanagement.model.LoginResponse;
import contactmanagement.model.LoginUserRequest;
import contactmanagement.model.WebResponse;
import contactmanagement.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureRestTestClient
class AuthControllerTest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("test");
        user.setPassword(passwordEncoder.encode("test1234"));
        user.setName("test");
        userRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void testLoginSuccess() {
        LoginUserRequest request = LoginUserRequest.builder()
                .username("test")
                .password("test1234")
                .build();

        WebResponse<LoginResponse> response = client.post()
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

        User user = userRepository.findById("test").orElseThrow();

        assertNotNull(user);
        assertNotNull(user.getToken());
        assertTrue(passwordEncoder.matches(request.getPassword(), user.getPassword()));
    }

    @Test
    void testLoginWrongPassword() {
        LoginUserRequest request = LoginUserRequest.builder()
                .username("test")
                .password("test1233")
                .build();

        WebResponse<LoginResponse> response = client.post()
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

        assertNotNull(response);
        assertEquals("Username or password wrong", response.getErrors());
    }

    @Test
    void testLoginUserNotFound() {
        LoginUserRequest request = LoginUserRequest.builder()
                .username("testis")
                .password("test1233")
                .build();

        WebResponse<LoginResponse> response = client.post()
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

        assertNotNull(response);
        assertEquals("Username or password wrong", response.getErrors());
    }

    @Test
    void testLoginUserInvalid() {
        LoginUserRequest request = LoginUserRequest.builder()
                .username("testis")
                .password("")
                .build();

        WebResponse<LoginResponse> response = client.post()
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

        assertNotNull(response);
        assertNotNull(response.getErrors());
    }
}