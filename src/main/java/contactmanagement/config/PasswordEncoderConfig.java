package contactmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration for password hashing.
 *
 * <p>Only {@code spring-security-crypto} is used; the full Spring Security filter chain is
 * not part of this project.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Provides the application-wide password encoder.
     *
     * @return a BCrypt encoder using the default strength (10), which salts every hash
     */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}