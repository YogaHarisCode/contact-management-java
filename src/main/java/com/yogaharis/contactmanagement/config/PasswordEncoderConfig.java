package com.yogaharis.contactmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring configuration that exposes the password hashing infrastructure.
 *
 * @author Yoga Harisistianugroho
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Creates the {@link PasswordEncoder} used to hash user passwords.
     *
     * <p>BCrypt only uses the first 72 bytes of its input, which is why password
     * length is limited with {@code @MaxBytes(72)} on the request DTO.</p>
     *
     * @return a BCrypt-based {@link PasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}