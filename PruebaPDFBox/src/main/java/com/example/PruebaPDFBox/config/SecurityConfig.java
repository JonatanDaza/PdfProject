package com.example.PruebaPDFBox.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()  // TODO público
                )
                .csrf(AbstractHttpConfigurer::disable); // Desactiva CSRF para APIs o POST

        return http.build();
    }
}
