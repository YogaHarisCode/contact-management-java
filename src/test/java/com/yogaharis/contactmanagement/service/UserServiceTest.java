package com.yogaharis.contactmanagement.service;

import com.yogaharis.contactmanagement.dto.RegisterUserRequest;
import com.yogaharis.contactmanagement.dto.UpdateUserRequest;
import com.yogaharis.contactmanagement.dto.UserResponse;
import com.yogaharis.contactmanagement.entity.User;
import com.yogaharis.contactmanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService} (register, get current user, and update user). The repository and
 * password encoder are mocked, so no Spring context or database is involved.
 *
 * @author Yoga Harisistianugroho
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private static final String USERNAME = "JhonDoe";
    private static final String RAW_PASSWORD = "Rahasia123";
    private static final String HASH_PASSWORD = "hash-password";
    private static final String NAME = "Jhon Doe";
    private static final String TOKEN = "Valid-Token";
    private static final Long TOKEN_EXPIRED_AT = 1_893_456_000_000L;
    private static final String NEW_NAME = "Jhon Doe Updated";
    private static final String NEW_PASSWORD = "NewPassword123";
    private static final String NEW_HASH_PASSWORD = "new-hash-password";

    // ---------------------------------------------------------- REGISTER
    @Test
    void testRegisterSuccess() {
        // arrange
        RegisterUserRequest request = validRequest();

        when(userRepository.existsById(USERNAME))
                .thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD))
                .thenReturn(HASH_PASSWORD);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act
        UserResponse response = userService.register(request);

        // assert
        InOrder inOrder = inOrder(userRepository, passwordEncoder);
        inOrder.verify(userRepository).existsById(USERNAME);
        inOrder.verify(passwordEncoder).encode(RAW_PASSWORD);

        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

        inOrder.verify(userRepository).saveAndFlush(userArgumentCaptor.capture());

        User captorValue = userArgumentCaptor.getValue();

        assertEquals(USERNAME, captorValue.getUsername());
        assertEquals(NAME, captorValue.getName());
        assertNotEquals(RAW_PASSWORD, captorValue.getPassword());
        assertEquals(HASH_PASSWORD, captorValue.getPassword());

        assertEquals(USERNAME, response.getUsername());
        assertEquals(NAME, response.getName());
    }

    @Test
    void testRegisterUsernameAlreadyExists() {
        // arrange
        RegisterUserRequest request = validRequest();

        when(userRepository.existsById(USERNAME))
                .thenReturn(true);

        // act & assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userService.register(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Username already exists", exception.getReason());

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    // ---------------------------------------------------------- GET
    /**
     * The user arrives already authenticated, so the service must only map it to a response
     * and must not touch the repository or the password encoder.
     */
    @Test
    void testGetCurrentUserSuccess() {
        User user = new User();
        user.setUsername(USERNAME);
        user.setPassword(HASH_PASSWORD);
        user.setName(NAME);

        UserResponse response = userService.get(user);

        assertEquals(USERNAME, response.getUsername());
        assertEquals(NAME, response.getName());

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    // ---------------------------------------------------------- UPDATE
    /** Only the name is provided: the password is not re-hashed and the token fields stay as they were. */
    @Test
    void testUpdateNameOnlySuccess() {
        // arrange
        User user = currentUser();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .name(NEW_NAME)
                .build();

        when(userRepository.findById(USERNAME))
                .thenReturn(Optional.of(user));
        // save returns the entity it was given, like the real repository does
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act
        UserResponse response = userService.update(user, request);

        // assert
        assertEquals(USERNAME, response.getUsername());
        assertEquals(NEW_NAME, response.getName());

        assertEquals(NEW_NAME, user.getName());
        assertEquals(HASH_PASSWORD, user.getPassword());
        assertEquals(TOKEN, user.getToken());
        assertEquals(TOKEN_EXPIRED_AT, user.getTokenExpiredAt());

        verify(userRepository).save(user);
        verifyNoInteractions(passwordEncoder);
    }

    /** Only the password is provided: it is stored as the encoder's hash, never as the raw value, and the name is untouched. */
    @Test
    void testUpdatePasswordOnlySuccess() {
        // arrange
        User user = currentUser();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .password(NEW_PASSWORD)
                .build();

        when(userRepository.findById(USERNAME))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.encode(NEW_PASSWORD))
                .thenReturn(NEW_HASH_PASSWORD);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act
        UserResponse response = userService.update(user, request);

        // assert
        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userArgumentCaptor.capture());

        User saved = userArgumentCaptor.getValue();

        assertEquals(NEW_HASH_PASSWORD, saved.getPassword());
        assertNotEquals(NEW_PASSWORD, saved.getPassword());
        assertEquals(NAME, saved.getName());
        assertEquals(TOKEN, saved.getToken());
        assertEquals(TOKEN_EXPIRED_AT, saved.getTokenExpiredAt());

        assertEquals(USERNAME, response.getUsername());
        assertEquals(NAME, response.getName());

        verify(passwordEncoder).encode(NEW_PASSWORD);
    }

    /** The user is reloaded first, then the password is hashed, then the entity is saved, in that order. */
    @Test
    void testUpdateBothNameAndPasswordSuccess() {
        // arrange
        User user = currentUser();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .name(NEW_NAME)
                .password(NEW_PASSWORD)
                .build();

        when(userRepository.findById(USERNAME))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.encode(NEW_PASSWORD))
                .thenReturn(NEW_HASH_PASSWORD);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act
        UserResponse response = userService.update(user, request);

        // assert
        InOrder inOrder = inOrder(userRepository, passwordEncoder);
        inOrder.verify(userRepository).findById(USERNAME);
        inOrder.verify(passwordEncoder).encode(NEW_PASSWORD);

        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
        inOrder.verify(userRepository).save(userArgumentCaptor.capture());

        User saved = userArgumentCaptor.getValue();

        assertEquals(NEW_NAME, saved.getName());
        assertEquals(NEW_HASH_PASSWORD, saved.getPassword());
        assertEquals(USERNAME, saved.getUsername());
        assertEquals(TOKEN, saved.getToken());
        assertEquals(TOKEN_EXPIRED_AT, saved.getTokenExpiredAt());

        assertEquals(USERNAME, response.getUsername());
        assertEquals(NEW_NAME, response.getName());
    }

    /** With nothing to change, the service answers from the given user without touching the repository or encoder. */
    @Test
    void testUpdateNoOpWhenAllFieldNull() {
        // arrange
        User user = currentUser();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .build();

        // act
        UserResponse response = userService.update(user, request);

        // assert
        assertEquals(USERNAME, response.getUsername());
        assertEquals(NAME, response.getName());

        assertEquals(NAME, user.getName());
        assertEquals(HASH_PASSWORD, user.getPassword());
        assertEquals(TOKEN, user.getToken());
        assertEquals(TOKEN_EXPIRED_AT, user.getTokenExpiredAt());

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    /**
     * Regression test: the user passed in may be stale (detached), so the service must change the
     * entity loaded from the repository and never save the stale one, or a token issued by a
     * concurrent login would be overwritten.
     */
    @Test
    void testUpdateModifiesLoadedEntityNotStaleOne() {
        // arrange
        User stale = currentUser();

        // the row as it is now: a login issued a newer token after `stale` was loaded
        User loaded = currentUser();
        loaded.setToken("Token-Issued-After-Authentication");
        loaded.setTokenExpiredAt(TOKEN_EXPIRED_AT + 1);

        UpdateUserRequest request = UpdateUserRequest.builder()
                .name(NEW_NAME)
                .build();

        when(userRepository.findById(USERNAME))
                .thenReturn(Optional.of(loaded));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act
        userService.update(stale, request);

        // assert
        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userArgumentCaptor.capture());

        assertSame(loaded, userArgumentCaptor.getValue());
        assertEquals(NEW_NAME, loaded.getName());
        assertEquals("Token-Issued-After-Authentication", loaded.getToken());
        assertEquals(TOKEN_EXPIRED_AT + 1, loaded.getTokenExpiredAt());
        assertEquals(NAME, stale.getName());
    }

    /** The user was deleted after authentication: respond 401, and neither hash the password nor save anything. */
    @Test
    void testUpdateUserNoLongerExists() {
        // arrange
        UpdateUserRequest request = UpdateUserRequest.builder()
                .name(NEW_NAME)
                .password(NEW_PASSWORD)
                .build();

        when(userRepository.findById(USERNAME))
                .thenReturn(Optional.empty());

        // act & assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.update(currentUser(), request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Unauthorized", exception.getReason());

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    // ---------------------------------------------------------- HELPER
    private RegisterUserRequest validRequest(){
        return RegisterUserRequest.builder()
                .username(USERNAME)
                .password(RAW_PASSWORD)
                .name(NAME)
                .build();
    }

    /** Builds a user with a fixed token expiry, so assertions do not depend on the current time. */
    private User currentUser(){
        User user = new User();
        user.setUsername(USERNAME);
        user.setPassword(HASH_PASSWORD);
        user.setName(NAME);
        user.setToken(TOKEN);
        user.setTokenExpiredAt(TOKEN_EXPIRED_AT);
        return user;
    }
}