package com.yogaharis.contactmanagement.service;

import com.yogaharis.contactmanagement.dto.LoginUserRequest;
import com.yogaharis.contactmanagement.dto.TokenResponse;
import com.yogaharis.contactmanagement.entity.User;
import com.yogaharis.contactmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for {@link AuthService#login(LoginUserRequest)}.
 *
 * <p>Runs without a Spring context: {@link UserRepository}, {@link PasswordEncoder} and
 * {@link Clock} are Mockito mocks. The clock is mocked so the token expiry can be asserted exactly.</p>
 *
 * @author Yoga Harisistianugroho
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    // Mocked so each test controls "now"; AuthService must read time only through clock.instant().
    @Mock
    private Clock clock;

    private AuthService authService;

    private static final String HASH_PASSWORD = "hash-password";
    private static final String DUMMY_HASH = "dummy-hash";
    private static final String UNAUTHORIZED_MESSAGE = "Username or password wrong";
    // Must match the token validity defined in AuthService.
    private static final Duration TOKEN_VALIDITY = Duration.ofDays(30);
    // Fixed point in time returned by the mocked clock.
    private static final Instant LOGIN_TIME = Instant.parse("2030-01-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        // AuthService hashes a dummy password in its constructor (timing equalization).
        Mockito.lenient().when(passwordEncoder.encode(Mockito.anyString())).thenReturn(DUMMY_HASH);
        authService = new AuthService(userRepository, passwordEncoder, clock);
        // Forget the encode() call made by the constructor, so verifyNoMoreInteractions only sees login().
        Mockito.clearInvocations(passwordEncoder);
    }

    // ---------------------------------------------------------- LOGIN

    /**
     * Valid credentials: the user is loaded, the password is checked, then a UUID token with an
     * expiry of "now + 30 days" is saved. The stored password hash must stay unchanged.
     */
    @Test
    void testLoginSuccess() {
        // arrange
        LoginUserRequest request = LoginUserRequest.builder()
                .username("JohnDoe")
                .password("Rahasia123")
                .build();
        User user = createUser();

        Mockito.when(clock.instant()).thenReturn(LOGIN_TIME);
        Mockito.when(userRepository.findById(request.getUsername())).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches(request.getPassword(), HASH_PASSWORD)).thenReturn(true);

        // act
        TokenResponse response = authService.login(request);

        // assert
        InOrder inOrder = Mockito.inOrder(userRepository, passwordEncoder);
        inOrder.verify(userRepository).findById(request.getUsername());
        inOrder.verify(passwordEncoder).matches(request.getPassword(), HASH_PASSWORD);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        inOrder.verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertEquals("JohnDoe", saved.getUsername());
        assertEquals(HASH_PASSWORD, saved.getPassword());
        assertDoesNotThrow(() -> UUID.fromString(saved.getToken()));
        assertEquals(LOGIN_TIME.plus(TOKEN_VALIDITY).toEpochMilli(), saved.getTokenExpiredAt());

        assertEquals(saved.getToken(), response.getToken());
        assertEquals(saved.getTokenExpiredAt(), response.getTokenExpiredAt());
    }

    /**
     * Unknown username: the password is still checked once, against the dummy hash, so the
     * timing matches a wrong password. The result is a 401, nothing is saved and the clock is not used.
     */
    @Test
    void testLoginUserNotFound() {
        // arrange
        LoginUserRequest request = LoginUserRequest.builder()
                .username("UserTidakAda")
                .password("Rahasia123")
                .build();

        Mockito.when(userRepository.findById(request.getUsername())).thenReturn(Optional.empty());

        // act
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> authService.login(request));

        // assert
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals(UNAUTHORIZED_MESSAGE, exception.getReason());

        Mockito.verify(userRepository).findById(request.getUsername());
        Mockito.verifyNoMoreInteractions(userRepository);
        Mockito.verify(passwordEncoder).matches(request.getPassword(), DUMMY_HASH);
        Mockito.verifyNoMoreInteractions(passwordEncoder);
        Mockito.verifyNoInteractions(clock);
    }

    /**
     * Even if the check against the dummy hash were to return {@code true}, a login for an
     * unknown username must still be rejected.
     */
    @Test
    void testLoginUserNotFoundIgnoresDummyHashMatch() {
        // arrange
        LoginUserRequest request = LoginUserRequest.builder()
                .username("UserTidakAda")
                .password("Rahasia123")
                .build();

        Mockito.when(userRepository.findById(request.getUsername())).thenReturn(Optional.empty());
        // Simulates a match on the dummy hash, which should never grant access.
        Mockito.when(passwordEncoder.matches(request.getPassword(), DUMMY_HASH)).thenReturn(true);

        // act
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> authService.login(request));

        // assert
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals(UNAUTHORIZED_MESSAGE, exception.getReason());

        Mockito.verify(userRepository).findById(request.getUsername());
        Mockito.verifyNoMoreInteractions(userRepository);
        Mockito.verifyNoInteractions(clock);
    }

    /**
     * Wrong password for an existing user: 401 with the same message as an unknown username,
     * nothing is saved and no token is set on the user.
     */
    @Test
    void testLoginWrongPassword() {
        // arrange
        LoginUserRequest request = LoginUserRequest.builder()
                .username("JohnDoe")
                .password("passwordsalah")
                .build();
        User user = createUser();

        Mockito.when(userRepository.findById(request.getUsername())).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches(request.getPassword(), HASH_PASSWORD)).thenReturn(false);

        // act
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> authService.login(request));

        // assert
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals(UNAUTHORIZED_MESSAGE, exception.getReason());

        Mockito.verify(userRepository).findById(request.getUsername());
        Mockito.verifyNoMoreInteractions(userRepository);
        Mockito.verify(passwordEncoder).matches(request.getPassword(), HASH_PASSWORD);
        Mockito.verifyNoMoreInteractions(passwordEncoder);
        Mockito.verifyNoInteractions(clock);

        assertNull(user.getToken());
        assertNull(user.getTokenExpiredAt());
    }

    /**
     * Two logins at different times must give different tokens and different expiry times.
     * Also guards against the expiry being computed once at class load instead of per login.
     */
    @Test
    void testLoginTwiceRotatesTokenAndRefreshesExpiry() {
        // arrange
        LoginUserRequest request = LoginUserRequest.builder()
                .username("JohnDoe")
                .password("Rahasia123")
                .build();
        User user = createUser();
        Instant firstLogin = LOGIN_TIME;
        Instant secondLogin = LOGIN_TIME.plus(Duration.ofDays(1));

        // The first call returns firstLogin, the second call returns secondLogin.
        Mockito.when(clock.instant()).thenReturn(firstLogin, secondLogin);
        Mockito.when(userRepository.findById(request.getUsername())).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches(request.getPassword(), HASH_PASSWORD)).thenReturn(true);

        // act
        TokenResponse first = authService.login(request);
        TokenResponse second = authService.login(request);

        // assert
        assertNotEquals(first.getToken(), second.getToken());
        assertEquals(firstLogin.plus(TOKEN_VALIDITY).toEpochMilli(), first.getTokenExpiredAt());
        assertEquals(secondLogin.plus(TOKEN_VALIDITY).toEpochMilli(), second.getTokenExpiredAt());
        assertEquals(second.getToken(), user.getToken());
        assertEquals(second.getTokenExpiredAt(), user.getTokenExpiredAt());
    }

    // ---------------------------------------------------------- HELPER

    /**
     * Creates an existing user as it would be loaded from the database, with no token yet.
     *
     * @return a user whose password is {@code HASH_PASSWORD}
     */
    private User createUser() {
        User user = new User();
        user.setUsername("JohnDoe");
        user.setPassword(HASH_PASSWORD);
        user.setName("John Doe");
        return user;
    }
}