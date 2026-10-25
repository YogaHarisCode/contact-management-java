package contactmanagement.resolver;

import contactmanagement.entity.User;
import contactmanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserArgumentResolverTest {

    private static final String TOKEN_HEADER = "X-API-TOKEN";
    private static final String TOKEN = "3c829e12-4217-48f5-a169-715a31bc0d6d";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserArgumentResolver resolver;

    /** Dummy handler method whose parameters are used to build real {@link MethodParameter}s. */
    @SuppressWarnings("unused")
    private void handler(User user, String other) {
    }

    @Test
    void shouldSupportParameterWhenTypeIsUser() throws Exception {
        assertTrue(resolver.supportsParameter(parameter(0)));
    }

    @Test
    void shouldNotSupportParameterWhenTypeIsNotUser() throws Exception {
        assertFalse(resolver.supportsParameter(parameter(1)));
    }

    @Test
    void shouldResolveUserWhenTokenIsValidAndNotExpired() throws Exception {
        User user = userWithToken(TOKEN, System.currentTimeMillis() + 60_000L);
        when(userRepository.findFirstByToken(TOKEN)).thenReturn(Optional.of(user));

        Object resolved = resolver.resolveArgument(parameter(0), null, requestWithToken(TOKEN), null);

        assertSame(user, resolved);
    }

    @Test
    void shouldThrowUnauthorizedWhenTokenHeaderIsMissing() {
        assertUnauthorized(() ->
                resolver.resolveArgument(parameter(0), null, requestWithToken(null), null));

        verifyNoInteractions(userRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void shouldThrowUnauthorizedWhenTokenHeaderIsBlank(String blankToken) {
        assertUnauthorized(() ->
                resolver.resolveArgument(parameter(0), null, requestWithToken(blankToken), null));

        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldThrowUnauthorizedWhenTokenIsNotFound() {
        when(userRepository.findFirstByToken("unknown-token")).thenReturn(Optional.empty());

        assertUnauthorized(() ->
                resolver.resolveArgument(parameter(0), null, requestWithToken("unknown-token"), null));
    }

    @Test
    void shouldThrowUnauthorizedWhenTokenIsExpired() {
        User user = userWithToken(TOKEN, System.currentTimeMillis() - 1_000L);
        when(userRepository.findFirstByToken(TOKEN)).thenReturn(Optional.of(user));

        assertUnauthorized(() ->
                resolver.resolveArgument(parameter(0), null, requestWithToken(TOKEN), null));
    }

    private void assertUnauthorized(Executable executable) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, executable);

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Unauthorized", exception.getReason());
    }

    private MethodParameter parameter(int index) throws NoSuchMethodException {
        Method method = UserArgumentResolverTest.class
                .getDeclaredMethod("handler", User.class, String.class);
        return new MethodParameter(method, index);
    }

    private static ServletWebRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) {
            request.addHeader(TOKEN_HEADER, token);
        }
        return new ServletWebRequest(request);
    }

    private static User userWithToken(String token, long expiredAt) {
        User user = new User();
        user.setUsername("johndoe");
        user.setPassword("bcrypt-hash");
        user.setName("John Doe");
        user.setToken(token);
        user.setTokenExpiredAt(expiredAt);
        return user;
    }
}