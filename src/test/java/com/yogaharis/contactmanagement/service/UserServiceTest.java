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

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService} (register and get current user). The repository and
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
    @Test
    void testUpdateNameOnlySuccess() {
        // arrange
        User user = new User();
        user.setUsername("JohnDoe");
        user.setPassword("Hash-Password");
        user.setName("John doe");
        user.setToken("Valid-Token");
        user.setTokenExpiredAt(Instant.now().plus(Duration.ofDays(1)).toEpochMilli());

        String oldName = user.getName();

        UpdateUserRequest request = UpdateUserRequest.builder()
                .name("John Doe Updated")
                .build();

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act
        UserResponse response = userService.update(user, request);

        // assert
        assertNotNull(response);
        assertEquals(user.getUsername(), response.getUsername());
        assertEquals(user.getName(), response.getName());
        assertNotEquals(oldName, user.getName());
        assertEquals("JohnDoe", user.getUsername());
        assertEquals("John Doe Updated", user.getName());
        assertEquals("Hash-Password", user.getPassword());
        assertEquals("Valid-Token", user.getToken());
        assertTrue(user.getTokenExpiredAt() > Instant.now().toEpochMilli());

        verify(userRepository).save(user);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void testUpdatePasswordOnlySuccess() {
        // arrange
        User user = new User();
        user.setUsername("JohnDoe");
        user.setPassword("Hash-Password");
        user.setName("John Doe");
        user.setToken("Valid-Token");
        user.setTokenExpiredAt(Instant.now().plus(Duration.ofDays(1)).toEpochMilli());

        String oldPassword = user.getPassword();

        UpdateUserRequest request = UpdateUserRequest.builder()
                .password("new-password")
                .build();

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("Hash-New-Password");

        // act
        UserResponse response = userService.update(user, request);

        // assert
        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userArgumentCaptor.capture());

        User captorValue = userArgumentCaptor.getValue();

        assertEquals("Hash-New-Password", captorValue.getPassword());

        assertNotNull(response);
        assertNotEquals(oldPassword, user.getPassword());
        assertEquals("JohnDoe", user.getUsername());
        assertEquals("John Doe", user.getName());
        assertEquals("Hash-New-Password", user.getPassword());
        assertEquals("Valid-Token", user.getToken());
        assertTrue(user.getTokenExpiredAt() > Instant.now().toEpochMilli());

        verify(passwordEncoder).encode(request.getPassword());
    }

    @Test
    void testUpdateBothNameAndPasswordSuccess() {
        // arrange
        User user = new User();
        user.setUsername("JohnDoe");
        user.setPassword("Hash-Password");
        user.setName("John Doe");
        user.setToken("Valid-Token");
        user.setTokenExpiredAt(Instant.now().plus(Duration.ofDays(1)).toEpochMilli());

        String oldName = user.getName();
        String oldPassword = user.getPassword();

        UpdateUserRequest request = UpdateUserRequest.builder()
                .name("John Doe Updated")
                .password("new-password")
                .build();

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("Hash-New-Password");

        // act
        UserResponse response = userService.update(user, request);

        // assert
        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userArgumentCaptor.capture());

        User captorValue = userArgumentCaptor.getValue();

        assertEquals("Hash-New-Password", captorValue.getPassword());

        assertNotNull(response);
        assertNotEquals(oldName, user.getName());
        assertNotEquals(oldPassword, user.getPassword());
        assertEquals("JohnDoe", user.getUsername());
        assertEquals("John Doe Updated", user.getName());
        assertEquals("Hash-New-Password", user.getPassword());
        assertEquals("Valid-Token", user.getToken());
        assertTrue(user.getTokenExpiredAt() > Instant.now().toEpochMilli());

        verify(passwordEncoder).encode(request.getPassword());
    }

    @Test
    void testUpdateNoOpWhenAllFieldNull() {
        // arrange
        User user = new User();
        user.setUsername("JohnDoe");
        user.setPassword("Hash-Password");
        user.setName("John Doe");
        user.setToken("Valid-Token");
        user.setTokenExpiredAt(Instant.now().plus(Duration.ofDays(1)).toEpochMilli());

        UpdateUserRequest request = UpdateUserRequest.builder()
                .build();

        // act
        UserResponse response = userService.update(user, request);

        // assert
        assertNotNull(response);
        assertEquals("JohnDoe", user.getUsername());
        assertEquals("John Doe", user.getName());
        assertEquals("Hash-Password", user.getPassword());
        assertEquals("Valid-Token", user.getToken());
        assertTrue(user.getTokenExpiredAt() > Instant.now().toEpochMilli());

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    // ---------------------------------------------------------- HELPER
    private RegisterUserRequest validRequest(){
        return RegisterUserRequest.builder()
                .username(USERNAME)
                .password(RAW_PASSWORD)
                .name(NAME)
                .build();
    }
}