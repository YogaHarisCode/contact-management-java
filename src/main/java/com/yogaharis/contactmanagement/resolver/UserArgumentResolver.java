package com.yogaharis.contactmanagement.resolver;

import com.yogaharis.contactmanagement.entity.User;
import com.yogaharis.contactmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

/**
 * Resolves controller method parameters of type {@link User} from the {@code X-API-TOKEN} request header.
 *
 * <p>A controller method that declares a {@code User} parameter is only reachable with a known,
 * unexpired token. Any other request is rejected with {@code 401 UNAUTHORIZED}.</p>
 *
 * @author Yoga Harisistianugroho
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;

    private static final HttpStatusCode UNAUTHORIZED_CODE = HttpStatus.UNAUTHORIZED;
    private static final String UNAUTHORIZED_MESSAGE = "Unauthorized";

    /**
     * Tells Spring MVC that this resolver handles parameters of type {@link User}.
     *
     * @param parameter the controller method parameter being inspected
     * @return {@code true} if the parameter type is exactly {@code User}
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(User.class);
    }

    /**
     * Finds the user that owns the token in the {@code X-API-TOKEN} header.
     *
     * @param parameter     the controller method parameter being resolved
     * @param mavContainer  the model and view container of the current request (not used)
     * @param webRequest    the current request, used to read the {@code X-API-TOKEN} header
     * @param binderFactory the data binder factory (not used)
     * @return the authenticated user
     * @throws ResponseStatusException with status {@code 401 UNAUTHORIZED} and message "Unauthorized"
     *                                 when the header is missing or blank, no user owns the token,
     *                                 or the token has expired
     */
    @Override
    public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        String header = webRequest.getHeader("X-API-TOKEN");
        // A missing or blank header means the client did not authenticate.
        if (header == null || header.isBlank()){
            log.debug("Authentication rejected: reason=missing_token");
            throw new ResponseStatusException(UNAUTHORIZED_CODE, UNAUTHORIZED_MESSAGE);
        }

        User user = userRepository.findByToken(header)
                .orElseThrow(() -> {
                    log.warn("Authentication rejected: reason=unknown_token");
                    return new ResponseStatusException(UNAUTHORIZED_CODE, UNAUTHORIZED_MESSAGE);
                });

        // A token without an expiry is invalid, as is one whose expiry time has passed.
        if (user.getTokenExpiredAt() == null || user.getTokenExpiredAt() < Instant.now().toEpochMilli()){
            log.debug("Authentication rejected: reason=token_expired_or_no_expiry, username={}", user.getUsername());
            throw new ResponseStatusException(UNAUTHORIZED_CODE, UNAUTHORIZED_MESSAGE);
        }

        return user;
    }
}