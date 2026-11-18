package com.yogaharis.contactmanagement.service;

import com.yogaharis.contactmanagement.dto.RegisterUserRequest;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UserService}. The repository and password encoder are mocked,
 * so no Spring context or database is involved.
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

        Mockito.when(userRepository.existsById(USERNAME))
                        .thenReturn(false);
        Mockito.when(passwordEncoder.encode(RAW_PASSWORD))
                .thenReturn(HASH_PASSWORD);
        Mockito.when(userRepository.saveAndFlush(Mockito.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act
        UserResponse response = userService.register(request);

        // assert
        InOrder inOrder = Mockito.inOrder(userRepository, passwordEncoder);
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

        Mockito.when(userRepository.existsById(USERNAME))
                .thenReturn(true);

        // act & assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userService.register(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Username already exists", exception.getReason());

        Mockito.verify(passwordEncoder, Mockito.never()).encode(Mockito.anyString());
        Mockito.verify(userRepository, Mockito.never()).saveAndFlush(Mockito.any(User.class));
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