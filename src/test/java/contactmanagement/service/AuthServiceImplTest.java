package contactmanagement.service;

import contactmanagement.entity.User;
import contactmanagement.model.LoginResponse;
import contactmanagement.model.LoginUserRequest;
import contactmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String DUMMY_HASH = "dummy-hash";
    private static final String USER_HASH = "user-bcrypt-hash";
    private static final String INVALID_CREDENTIALS = "Username or password wrong";
    private static final long TOKEN_TTL_MILLIS = Duration.ofDays(30).toMillis();

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // The constructor hashes a random string to build the dummy hash, so encode() must be
        // stubbed before the service is created (@InjectMocks would leave the dummy hash null).
        when(passwordEncoder.encode(anyString())).thenReturn(DUMMY_HASH);
        authService = new AuthServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    void shouldReturnTokenAndExpiryWhenCredentialsAreValid() {
        User user = existingUser();
        when(userRepository.findById("test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("test1234", USER_HASH)).thenReturn(true);

        LoginResponse response = authService.login(request("test", "test1234"));

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getTokenExpiredAt());
    }

    @Test
    void shouldGenerateValidUuidTokenWhenLoginSucceeds() {
        when(userRepository.findById("test")).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("test1234", USER_HASH)).thenReturn(true);

        LoginResponse response = authService.login(request("test", "test1234"));

        assertDoesNotThrow(() -> UUID.fromString(response.getToken()));
    }

    @Test
    void shouldSetTokenExpiryThirtyDaysAheadWhenLoginSucceeds() {
        when(userRepository.findById("test")).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("test1234", USER_HASH)).thenReturn(true);

        long before = System.currentTimeMillis();
        LoginResponse response = authService.login(request("test", "test1234"));
        long after = System.currentTimeMillis();

        assertTrue(response.getTokenExpiredAt() >= before + TOKEN_TTL_MILLIS);
        assertTrue(response.getTokenExpiredAt() <= after + TOKEN_TTL_MILLIS);
    }

    @Test
    void shouldStoreTokenAndExpiryOnUserEntityWhenLoginSucceeds() {
        User user = existingUser();
        when(userRepository.findById("test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("test1234", USER_HASH)).thenReturn(true);

        LoginResponse response = authService.login(request("test", "test1234"));

        // The service relies on JPA dirty checking inside @Transactional, so the managed
        // entity itself must carry the new values and match what is returned to the client.
        assertEquals(response.getToken(), user.getToken());
        assertEquals(response.getTokenExpiredAt(), user.getTokenExpiredAt());
    }

    @Test
    void shouldGenerateDifferentTokenOnEachLogin() {
        when(userRepository.findById("test")).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("test1234", USER_HASH)).thenReturn(true);

        LoginResponse first = authService.login(request("test", "test1234"));
        LoginResponse second = authService.login(request("test", "test1234"));

        assertNotEquals(first.getToken(), second.getToken());
    }

    @Test
    void shouldThrowUnauthorizedWhenPasswordDoesNotMatch() {
        User user = existingUser();
        when(userRepository.findById("test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", USER_HASH)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(request("test", "wrong-password")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals(INVALID_CREDENTIALS, exception.getReason());
    }

    @Test
    void shouldNotIssueTokenWhenPasswordDoesNotMatch() {
        User user = existingUser();
        when(userRepository.findById("test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", USER_HASH)).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> authService.login(request("test", "wrong-password")));

        assertNull(user.getToken());
        assertNull(user.getTokenExpiredAt());
    }

    @Test
    void shouldThrowUnauthorizedWhenUserIsNotFound() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("test1234", DUMMY_HASH)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(request("unknown", "test1234")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals(INVALID_CREDENTIALS, exception.getReason());
    }

    @Test
    void shouldStillCompareAgainstDummyHashWhenUserIsNotFound() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("test1234", DUMMY_HASH)).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> authService.login(request("unknown", "test1234")));

        // Guards against username enumeration via response time: BCrypt must run either way.
        verify(passwordEncoder).matches("test1234", DUMMY_HASH);
    }

    @Test
    void shouldReturnSameErrorMessageForUnknownUserAndWrongPassword() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());
        when(userRepository.findById("test")).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("test1234", DUMMY_HASH)).thenReturn(false);
        when(passwordEncoder.matches("wrong-password", USER_HASH)).thenReturn(false);

        ResponseStatusException unknownUser = assertThrows(ResponseStatusException.class,
                () -> authService.login(request("unknown", "test1234")));
        ResponseStatusException wrongPassword = assertThrows(ResponseStatusException.class,
                () -> authService.login(request("test", "wrong-password")));

        assertEquals(unknownUser.getStatusCode(), wrongPassword.getStatusCode());
        assertEquals(unknownUser.getReason(), wrongPassword.getReason());
    }

    private static User existingUser() {
        User user = new User();
        user.setUsername("test");
        user.setPassword(USER_HASH);
        user.setName("Test User");
        return user;
    }

    private static LoginUserRequest request(String username, String password) {
        return LoginUserRequest.builder()
                .username(username)
                .password(password)
                .build();
    }
}