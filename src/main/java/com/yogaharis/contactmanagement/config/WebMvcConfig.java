package com.yogaharis.contactmanagement.config;

import com.yogaharis.contactmanagement.resolver.UserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC configuration that registers custom controller argument resolvers.
 *
 * @author Yoga Harisistianugroho
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserArgumentResolver userArgumentResolver;

    /**
     * Registers {@link UserArgumentResolver} so controller methods can receive the
     * authenticated {@code User} as a parameter.
     *
     * @param resolvers the list of argument resolvers to add to
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userArgumentResolver);
    }
}