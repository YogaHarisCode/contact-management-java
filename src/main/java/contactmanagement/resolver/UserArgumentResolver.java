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

@Slf4j
@Component
@RequiredArgsConstructor
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String TOKEN_HEADER = "X-API-TOKEN";

    private final UserRepository userRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return User.class.equals(parameter.getParameterType());
    }

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

        // Fail closed: a token without an expiry time is treated as invalid.
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

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}