package contactmanagement.resolver;

import contactmanagement.entity.User;
import contactmanagement.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;

    private ResponseStatusException unauthorized(){
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(User.class);
    }

    @Override
    public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String header = request.getHeader("X-API-TOKEN");

        if (Objects.isNull(header)){
            throw unauthorized();
        }

        User user = userRepository.findFirstByToken(header)
                .orElseThrow(this::unauthorized);

        if (user.getTokenExpiredAt() < Instant.now().toEpochMilli()){
            throw unauthorized();
        }

        return user;
    }
}
