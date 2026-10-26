package contactmanagement.service;

import contactmanagement.entity.User;
import contactmanagement.model.UpdateUserRequest;
import contactmanagement.model.UserResponse;
import contactmanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserServiceImpl} (get-current-user and update-current-user), with the
 * repository and password encoder mocked.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String ORIGINAL_HASH = "original-bcrypt-hash";
    private static final String NEW_PASSWORD = "newpassword123";
    private static final String NEW_HASH = "new-bcrypt-hash";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    // ------------------------------------------------------------------ get

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

    // --------------------------------------------------------------- update

    @Test
    void shouldUpdateOnlyNameWhenOnlyNameIsProvided() {
        User user = authenticatedUser("johndoe", "John Doe");
        UpdateUserRequest request = UpdateUserRequest.builder().name("Jane Doe").build();

        UserResponse response = userService.update(user, request);

        assertEquals("Jane Doe", user.getName());
        assertEquals(ORIGINAL_HASH, user.getPassword());
        assertEquals("Jane Doe", response.getName());
        verify(userRepository).save(user);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldHashAndStoreOnlyPasswordWhenOnlyPasswordIsProvided() {
        User user = authenticatedUser("johndoe", "John Doe");
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_HASH);

        UserResponse response = userService.update(user, UpdateUserRequest.builder()
                .password(NEW_PASSWORD)
                .build());

        assertEquals(NEW_HASH, user.getPassword());
        assertEquals("John Doe", user.getName());
        assertEquals("John Doe", response.getName());
        verify(passwordEncoder).encode(NEW_PASSWORD);
        verify(userRepository).save(user);
    }

    @Test
    void shouldUpdateNameAndPasswordWhenBothAreProvided() {
        User user = authenticatedUser("johndoe", "John Doe");
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_HASH);

        UserResponse response = userService.update(user, UpdateUserRequest.builder()
                .name("Jane Doe")
                .password(NEW_PASSWORD)
                .build());

        assertEquals("Jane Doe", user.getName());
        assertEquals(NEW_HASH, user.getPassword());
        assertEquals("Jane Doe", response.getName());
        verify(userRepository).save(user);
    }

    @Test
    void shouldKeepCurrentValuesWhenNoFieldIsProvided() {
        User user = authenticatedUser("johndoe", "John Doe");

        UserResponse response = userService.update(user, new UpdateUserRequest());

        assertEquals("John Doe", user.getName());
        assertEquals(ORIGINAL_HASH, user.getPassword());
        assertEquals("John Doe", response.getName());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldNotStorePlainTextPasswordWhenPasswordIsUpdated() {
        User user = authenticatedUser("johndoe", "John Doe");
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_HASH);

        userService.update(user, UpdateUserRequest.builder().password(NEW_PASSWORD).build());

        assertNotEquals(NEW_PASSWORD, user.getPassword());
    }

    @Test
    void shouldNotChangeUsernameOrTokenWhenProfileIsUpdated() {
        User user = authenticatedUser("johndoe", "John Doe");
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_HASH);
        Long expiredAt = user.getTokenExpiredAt();

        userService.update(user, UpdateUserRequest.builder()
                .name("Jane Doe")
                .password(NEW_PASSWORD)
                .build());

        assertEquals("johndoe", user.getUsername());
        assertEquals("token-must-not-leak", user.getToken());
        assertEquals(expiredAt, user.getTokenExpiredAt());
    }

    @Test
    void shouldReturnOnlyPublicFieldsWhenProfileIsUpdated() {
        User user = authenticatedUser("johndoe", "John Doe");
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_HASH);

        UserResponse response = userService.update(user, UpdateUserRequest.builder()
                .name("Jane Doe")
                .password(NEW_PASSWORD)
                .build());

        UserResponse expected = UserResponse.builder()
                .username("johndoe")
                .name("Jane Doe")
                .build();
        assertEquals(expected, response);
    }

    @Test
    void shouldNotSaveUserWhenPasswordEncodingFails() {
        User user = authenticatedUser("johndoe", "John Doe");
        when(passwordEncoder.encode(NEW_PASSWORD)).thenThrow(new IllegalArgumentException("encoding failed"));

        assertThrows(IllegalArgumentException.class,
                () -> userService.update(user, UpdateUserRequest.builder().password(NEW_PASSWORD).build()));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldPropagateExceptionWhenRepositorySaveFails() {
        User user = authenticatedUser("johndoe", "John Doe");
        when(userRepository.save(user)).thenThrow(new DataIntegrityViolationException("save failed"));

        assertThrows(DataIntegrityViolationException.class,
                () -> userService.update(user, UpdateUserRequest.builder().name("Jane Doe").build()));
    }

    private static User authenticatedUser(String username, String name) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(ORIGINAL_HASH);
        user.setName(name);
        user.setToken("token-must-not-leak");
        user.setTokenExpiredAt(System.currentTimeMillis() + 60_000L);
        return user;
    }
}