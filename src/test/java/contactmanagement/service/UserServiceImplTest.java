package contactmanagement.service;

import contactmanagement.entity.User;
import contactmanagement.model.UserResponse;
import contactmanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link UserServiceImpl} (get-current-user), with the repository and password
 * encoder mocked.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldReturnUsernameAndNameWhenUserIsGiven() {
        User user = authenticatedUser("johndoe", "John Doe");

        UserResponse response = userService.get(user);

        assertNotNull(response);
        assertEquals("johndoe", response.getUsername());
        assertEquals("John Doe", response.getName());
    }

    @Test
    void shouldReturnOnlyPublicFieldsWhenUserHasPasswordHashAndToken() {
        User user = authenticatedUser("johndoe", "John Doe");

        UserResponse response = userService.get(user);

        // UserResponse only has username and name, so equality proves nothing else is mapped.
        UserResponse expected = UserResponse.builder()
                .username("johndoe")
                .name("John Doe")
                .build();
        assertEquals(expected, response);
    }

    @Test
    void shouldNotAccessRepositoryOrPasswordEncoderWhenGettingCurrentUser() {
        userService.get(authenticatedUser("johndoe", "John Doe"));

        // The user is already resolved from the token, so no extra lookup or hashing is needed.
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    private static User authenticatedUser(String username, String name) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("bcrypt-hash-must-not-leak");
        user.setName(name);
        user.setToken("token-must-not-leak");
        user.setTokenExpiredAt(System.currentTimeMillis() + 60_000L);
        return user;
    }
}