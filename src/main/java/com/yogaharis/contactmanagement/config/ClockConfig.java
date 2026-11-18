package com.yogaharis.contactmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Exposes a {@link Clock} bean so time-dependent logic can be tested with a fixed or mocked clock.
 *
 * @author Yoga Harisistianugroho
 */
@Configuration
public class ClockConfig {

    /**
     * Provides the system clock in UTC.
     *
     * @return the UTC system clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}