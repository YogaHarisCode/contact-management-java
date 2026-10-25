package contactmanagement.resolver;

import contactmanagement.entity.User;
import contactmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Authenticates a request and injects the current {@link User} into controller methods.
 *
 * <p>Any controller method that declares a parameter of type {@link User} receives the user that
 * owns the token sent in the {@code X-API-TOKEN} header. The request is rejected with
 * {@code 401 Unauthorized} (message {@code "Unauthorized"}) when the header is missing or blank,
 * when no user holds the token, or when the token has expired or has no expiry time.
 *
 * <p>Authentication is therefore opt-in per endpoint: an endpoint is protected only if it
 * declares a {@link User} parameter. The resolver is registered in
 * {@link contactmanagement.config.WebConfiguration}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    /** Name of the request header that carries the authentication token. */
    private static final String TOKEN_HEADER = "X-API-TOKEN";

    /** Repository used to look up the user that owns a token. */
    private final UserRepository userRepository;

    /**
     * Tells Spring MVC that this resolver handles controller parameters of type {@link User}.
     *
     * @param parameter the controller method parameter being inspected
     * @return {@code true} when the parameter type is exactly {@link User}
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return User.class.equals(parameter.getParameterType());
    }

    /**
     * Resolves the authenticated user from the {@code X-API-TOKEN} header.
     *
     * @param parameter     the controller method parameter being resolved
     * @param mavContainer  the model and view container of the current request (not used)
     * @param webRequest    the current request, used to read the token header
     * @param binderFactory the data binder factory (not used)
     * @return the user that owns a valid, unexpired token
     * @throws ResponseStatusException with status 401 when the header is missing or blank, the
     *         token is unknown, or the token has expired or has no expiry time
     */
    @Override
    public @Nullable Object resolveArgument(MethodParameter parameter,
                                            @Nullable ModelAndViewContainer mavContainer,
                                            NativeWebRequest webRequest,
                                            @Nullable WebDataBinderFactory binderFactory) {
        String token = webRequest.getHeader(TOKEN_HEADER);

        // Rejects null, empty, and whitespace-only values before touching the database.
        if (!StringUtils.hasText(token)) {
            log.warn("Authentication rejected: reason=missing_token_header");
            throw unauthorized();
        }

        User user = userRepository.findFirstByToken(token)
                .orElseThrow(() -> {
                    // The token value is deliberately never logged.
                    log.warn("Authentication rejected: reason=token_not_found");
                    return unauthorized();
                });

        // tokenExpiredAt is epoch milliseconds (UTC), so it is compared with the current epoch
        // milliseconds. Fail closed: a token without an expiry time is treated as invalid.
        Long expiredAt = user.getTokenExpiredAt();
        if (expiredAt == null || expiredAt < Instant.now().toEpochMilli()) {
            log.warn("Authentication rejected: reason={}, username={}, tokenExpiredAt={}",
                    (expiredAt == null) ? "token_without_expiry" : "token_expired",
                    user.getUsername(), expiredAt);
            throw unauthorized();
        }

        log.debug("Authentication succeeded: username={}", user.getUsername());
        return user;
    }

    /**
     * Builds the exception used for every authentication failure.
     *
     * <p>All failure causes share one message so the response does not reveal why the token was
     * rejected.
     *
     * @return a 401 exception carrying the standard {@code "Unauthorized"} message
     */
    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}