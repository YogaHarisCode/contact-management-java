package contactmanagement.config;

import contactmanagement.resolver.UserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC configuration of the application.
 *
 * <p>Registers the custom argument resolver that authenticates requests and injects the current
 * user into controller methods.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfiguration implements WebMvcConfigurer {

    /** Resolver that turns the {@code X-API-TOKEN} header into a {@code User} parameter. */
    private final UserArgumentResolver userArgumentResolver;

    /**
     * Adds {@link UserArgumentResolver} to the resolvers used for controller method parameters.
     *
     * @param resolvers the list of custom argument resolvers to extend
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userArgumentResolver);
    }
}