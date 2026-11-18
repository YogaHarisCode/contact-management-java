package com.yogaharis.contactmanagement.resolver;

import com.yogaharis.contactmanagement.entity.User;
import com.yogaharis.contactmanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link UserArgumentResolver}.
 *
 * <p>Runs without a Spring context: {@link UserRepository} is a Mockito mock and requests are
 * built with {@link MockHttpServletRequest}. No endpoint uses this resolver yet, so it is
 * tested directly.</p>
 *
 * @author Yoga Harisistianugroho
 */
@ExtendWith(MockitoExtension.class)
class UserArgumentResolverTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserArgumentResolver resolver;

    private static final String TOKEN_HEADER = "X-API-TOKEN";
    private static final String VALID_TOKEN = "valid-token";
    private static final String UNAUTHORIZED_MESSAGE = "Unauthorized";

    /** Only parameters of type {@code User} are supported. */
    @Test
    void testSupportsParameter() throws Exception {
        assertTrue(resolver.supportsParameter(parameterAt(0)));  // User
        assertFalse(resolver.supportsParameter(parameterAt(1))); // String
    }

    /** A known token that has not expired resolves to its user. */
    @Test
    void testResolveArgumentSuccess() throws Exception {
        User user = createUser(Instant.now().plus(Duration.ofHours(1)).toEpochMilli());
        Mockito.when(userRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(user));

        Object result = resolver.resolveArgument(parameterAt(0), null, requestWithToken(VALID_TOKEN), null);

        assertSame(user, result);
        Mockito.verify(userRepository).findByToken(VALID_TOKEN);
    }

    /** A missing, empty or blank header gives 401 without querying the database. */
    @ParameterizedTest(name = "[{index}] header = \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void testResolveArgumentMissingOrBlankToken(String token) throws Exception {
        MethodParameter parameter = parameterAt(0);
        ServletWebRequest request = requestWithToken(token);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(parameter, null, request, null));

        assertUnauthorized(exception);
        Mockito.verifyNoInteractions(userRepository);
    }

    /** A token that no user owns gives 401. */
    @Test
    void testResolveArgumentUnknownToken() throws Exception {
        Mockito.when(userRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        MethodParameter parameter = parameterAt(0);
        ServletWebRequest request = requestWithToken("unknown-token");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(parameter, null, request, null));

        assertUnauthorized(exception);
    }

    /** A token whose expiry time is in the past gives 401. */
    @Test
    void testResolveArgumentExpiredToken() throws Exception {
        User user = createUser(Instant.now().minusSeconds(1).toEpochMilli());
        Mockito.when(userRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(user));

        MethodParameter parameter = parameterAt(0);
        ServletWebRequest request = requestWithToken(VALID_TOKEN);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(parameter, null, request, null));

        assertUnauthorized(exception);
    }

    /** A token without an expiry time is treated as invalid and gives 401. */
    @Test
    void testResolveArgumentTokenWithoutExpiry() throws Exception {
        User user = createUser(null);
        Mockito.when(userRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(user));

        MethodParameter parameter = parameterAt(0);
        ServletWebRequest request = requestWithToken(VALID_TOKEN);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(parameter, null, request, null));

        assertUnauthorized(exception);
    }

    // ---------------------------------------------------------- HELPER

    /**
     * Asserts that the exception is the standard 401 with the message "Unauthorized".
     *
     * @param exception the exception thrown by the resolver
     */
    private void assertUnauthorized(ResponseStatusException exception) {
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals(UNAUTHORIZED_MESSAGE, exception.getReason());
    }

    /**
     * Creates a user that owns {@code VALID_TOKEN}.
     *
     * @param tokenExpiredAt the token expiry as epoch milliseconds, or {@code null} for none
     * @return the user
     */
    private User createUser(Long tokenExpiredAt) {
        User user = new User();
        user.setUsername("JohnDoe");
        user.setPassword("hash-password");
        user.setName("John Doe");
        user.setToken(VALID_TOKEN);
        user.setTokenExpiredAt(tokenExpiredAt);
        return user;
    }

    /**
     * Builds a request carrying the {@code X-API-TOKEN} header.
     *
     * @param token the header value, or {@code null} to send no header at all
     * @return the request wrapped for the resolver
     */
    private ServletWebRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) {
            request.addHeader(TOKEN_HEADER, token);
        }
        return new ServletWebRequest(request);
    }

    /**
     * Returns a parameter of {@link SampleHandler#handle(User, String)}.
     *
     * @param index {@code 0} for the {@code User} parameter, {@code 1} for the {@code String} parameter
     * @return the method parameter at that index
     * @throws NoSuchMethodException if the sample handler method cannot be found
     */
    private MethodParameter parameterAt(int index) throws NoSuchMethodException {
        Method method = SampleHandler.class.getDeclaredMethod("handle", User.class, String.class);
        return new MethodParameter(method, index);
    }

    /** Provides a method signature so tests can build {@link MethodParameter} instances. */
    private static class SampleHandler {
        @SuppressWarnings("unused")
        void handle(User user, String other) {
        }
    }
}