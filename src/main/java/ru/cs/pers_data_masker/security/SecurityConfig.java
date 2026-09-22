package ru.cs.pers_data_masker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Конфигурация Spring Security.
 *
 * <p>Идентификация системы-потребителя выполняется через
 * {@link SystemIdentificationFilter} (заголовок {@code X-System-Id}). Все запросы
 * разрешены на уровне Security — проверка «система включена» идёт в фильтре.
 * CSRF и сессии отключены (stateless API).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SystemIdentificationFilter systemIdentificationFilter;

    public SecurityConfig(SystemIdentificationFilter systemIdentificationFilter) {
        this.systemIdentificationFilter = systemIdentificationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(systemIdentificationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}