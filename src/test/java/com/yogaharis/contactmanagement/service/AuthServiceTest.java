package com.yogaharis.contactmanagement.service;

import com.yogaharis.contactmanagement.dto.LoginUserRequest;
import com.yogaharis.contactmanagement.dto.TokenResponse;
import com.yogaharis.contactmanagement.entity.User;
import com.yogaharis.contactmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
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
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link AuthService#login(LoginUserRequest)} and {@link AuthService#logout(User)}.
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
    // Message used by login only; logout rejections use "Unauthorized".
    private static final String UNAUTHORIZED_MESSAGE = "Username or password wrong";
    // Must match the token validity defined in AuthService.
    private static final Duration TOKEN_VALIDITY = Duration.ofDays(30);
    // Fixed point in time returned by the mocked clock.
    private static final Instant LOGIN_TIME = Instant.parse("2030-01-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        // AuthService hashes a dummy password in its constructor (timing equalization).
        lenient().when(passwordEncoder.encode(anyString())).thenReturn(DUMMY_HASH);
        authService = new AuthService(userRepository, passwordEncoder, clock);
        // Forget the encode() call made by the constructor, so later verifications only see calls made by the test.
        clearInvocations(passwordEncoder);
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

        when(clock.instant()).thenReturn(LOGIN_TIME);
        when(userRepository.findById(request.getUsername())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), HASH_PASSWORD)).thenReturn(true);

        // act
        TokenResponse response = authService.login(request);

        // assert
        InOrder inOrder = inOrder(userRepository, passwordEncoder);
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

        when(userRepository.findById(request.getUsername())).thenReturn(Optional.empty());

        // act
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> authService.login(request));

        // assert
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals(UNAUTHORIZED_MESSAGE, exception.getReason());

        verify(userRepository).findById(request.getUsername());
        verifyNoMoreInteractions(userRepository);
        verify(passwordEncoder).matches(request.getPassword(), DUMMY_HASH);
        verifyNoMoreInteractions(passwordEncoder);
        verifyNoInteractions(clock);
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

        when(userRepository.findById(request.getUsername())).thenReturn(Optional.empty());
        // Simulates a match on the dummy hash, which should never grant access.
        when(passwordEncoder.matches(request.getPassword(), DUMMY_HASH)).thenReturn(true);

        // act
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> authService.login(request));

        // assert
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals(UNAUTHORIZED_MESSAGE, exception.getReason());

        verify(userRepository).findById(request.getUsername());
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(clock);
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

        when(userRepository.findById(request.getUsername())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), HASH_PASSWORD)).thenReturn(false);

        // act
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> authService.login(request));

        // assert
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals(UNAUTHORIZED_MESSAGE, exception.getReason());

        verify(userRepository).findById(request.getUsername());
        verifyNoMoreInteractions(userRepository);
        verify(passwordEncoder).matches(request.getPassword(), HASH_PASSWORD);
        verifyNoMoreInteractions(passwordEncoder);
        verifyNoInteractions(clock);

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
        when(clock.instant()).thenReturn(firstLogin, secondLogin);
        when(userRepository.findById(request.getUsername())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), HASH_PASSWORD)).thenReturn(true);

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

    // ---------------------------------------------------------- LOGOUT

    /**
     * Valid logout: the user is reloaded, then saved with its token and expiry cleared. Name and password
     * hash stay unchanged, and neither the password encoder nor the clock is used.
     */
    @Test
    void testLogoutSuccess() {
        // arrange
        User user = createUserWithToken("Valid-Token");

        when(userRepository.findById("JohnDoe")).thenReturn(Optional.of(user));

        // act
        authService.logout(user);

        // assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        InOrder inOrder = inOrder(userRepository);
        inOrder.verify(userRepository).findById("JohnDoe");
        inOrder.verify(userRepository).save(userCaptor.capture());
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder, clock);

        User saved = userCaptor.getValue();
        assertNull(saved.getToken());
        assertNull(saved.getTokenExpiredAt());
        assertEquals("JohnDoe", saved.getUsername());
        assertEquals(HASH_PASSWORD, saved.getPassword());
        assertEquals("John Doe", saved.getName());
    }

    /**
     * The entity that gets saved is the one reloaded with {@code findById}, not the detached argument.
     * The two differ in password hash and name to prove which one is used: the reloaded values are kept
     * and the detached argument is left untouched.
     */
    @Test
    void testLogoutUsesManagedEntityNotDetachedArgument() {
        // arrange
        User detachedUser = createUserWithToken("Valid-Token");
        detachedUser.setPassword("Old-Hash-Password");

        User managedUser = createUserWithToken("Valid-Token");
        managedUser.setPassword("New-Hash-Password");
        managedUser.setName("John Doe Updated");

        when(userRepository.findById("JohnDoe")).thenReturn(Optional.of(managedUser));

        // act
        authService.logout(detachedUser);

        // assert
        assertNull(managedUser.getToken());
        assertNull(managedUser.getTokenExpiredAt());
        assertEquals("New-Hash-Password", managedUser.getPassword());
        assertEquals("John Doe Updated", managedUser.getName());
        assertEquals("Valid-Token", detachedUser.getToken());
        assertNotNull(detachedUser.getTokenExpiredAt());

        verify(userRepository).findById("JohnDoe");
        verify(userRepository).save(same(managedUser));
        verifyNoMoreInteractions(userRepository);
    }

    /** The user no longer exists: 401 "Unauthorized" and nothing is saved. */
    @Test
    void testLogoutUserNotFound() {
        // arrange
        User user = createUserWithToken("Valid-Token");

        when(userRepository.findById("JohnDoe")).thenReturn(Optional.empty());

        // act
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> authService.logout(user));

        // assert
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Unauthorized", exception.getReason());

        verify(userRepository).findById("JohnDoe");
        verifyNoMoreInteractions(userRepository);
    }

    /**
     * Regression test: the argument carries an old token but the database already holds a newer one from
     * another login. Logout is rejected with 401, the newer token is kept and nothing is saved.
     */
    @Test
    void testLogoutRejectedWhenTokenWasReplacedByNewLogin() {
        // arrange
        User staleUser = createUserWithToken("Old-Token");
        User managedUser = createUserWithToken("New-Token");

        when(userRepository.findById("JohnDoe")).thenReturn(Optional.of(managedUser));

        // act
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> authService.logout(staleUser));

        // assert
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Unauthorized", exception.getReason());
        assertEquals("New-Token", managedUser.getToken());
        assertNotNull(managedUser.getTokenExpiredAt());

        verify(userRepository).findById("JohnDoe");
        verifyNoMoreInteractions(userRepository);
    }

    /**
     * The reloaded user has no token any more (for example a concurrent logout already cleared it).
     * Logout is rejected with 401 and nothing is saved.
     */
    @Test
    void testLogoutRejectedWhenTokenAlreadyCleared() {
        // arrange
        User staleUser = createUserWithToken("Old-Token");
        User managedUser = createUser();

        when(userRepository.findById("JohnDoe")).thenReturn(Optional.of(managedUser));

        // act
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> authService.logout(staleUser));

        // assert
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Unauthorized", exception.getReason());

        verify(userRepository).findById("JohnDoe");
        verifyNoMoreInteractions(userRepository);
    }

    // ---------------------------------------------------------- HELPER

    /**
     * Creates an existing user like {@link #createUser()}, but with the given token and an expiry
     * one day after {@code LOGIN_TIME}.
     *
     * @param token the token to set on the user
     * @return a user that has a token and a token expiry
     */
    private User createUserWithToken(String token) {
        User user = createUser();
        user.setToken(token);
        user.setTokenExpiredAt(LOGIN_TIME.plus(Duration.ofDays(1)).toEpochMilli());
        return user;
    }

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