package contactmanagement.controller;

import contactmanagement.entity.User;
import contactmanagement.model.RegisterUserRequest;
import contactmanagement.model.UserResponse;
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
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureRestTestClient
class UserControllerTest {

    @Autowired
    private RestTestClient client;
    
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void testRegisterSuccess() {
        RegisterUserRequest request = RegisterUserRequest.builder()
                .username("test")
                .password("test1234")
                .name("test")
                .build();

        WebResponse<UserResponse> response = client.post()
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

        assertNotNull(response);
        assertEquals(request.getUsername(), response.getData().getUsername());
        assertEquals(request.getName(), response.getData().getName());
    }

    @Test
    void testRegisterDuplicateUsername() {
        User user = new User();
        user.setUsername("test");
        user.setPassword("test");
        user.setName("test");
        userRepository.save(user);

        RegisterUserRequest request = RegisterUserRequest.builder()
                .username("test")
                .password("test1234")
                .name("test")
                .build();

        WebResponse<UserResponse> response = client.post()
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

        assertNotNull(response);
        assertEquals("Username already exists", response.getErrors());
    }

    @Test
    void testRegisterInvalid() {
        RegisterUserRequest request = RegisterUserRequest.builder()
                .username("")
                .password("1234")
                .name("test")
                .build();

        WebResponse<UserResponse> response = client.post()
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

        assertNotNull(response);
        assertNotNull(response.getErrors());
    }
}