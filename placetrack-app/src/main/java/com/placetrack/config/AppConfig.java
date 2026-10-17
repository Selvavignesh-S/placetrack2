package com.placetrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Shared beans. M2 adds the login interceptor here (implements WebMvcConfigurer). */
@Configuration
public class AppConfig {

    /** BCrypt hashes passwords with a random salt; the same bean checks them at login. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
