package com.bancoxyz.bffweb.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        /**
         * EFT:
         * Los endpoints del BFF Web funcionan como OAuth2 Resource Server
         * y validan JWT emitidos por el auth-server.
         */
        @Bean
        @Order(1)
        SecurityFilterChain apiSecurityChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/**")
                                .csrf(AbstractHttpConfigurer::disable)
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/web/**")
                                                .hasAllAuthorities("SCOPE_banco.read", "SCOPE_canal.web")
                                                .anyRequest().denyAll())
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

                return http.build();
        }

        /**
         * Health e info quedan públicos para Docker/Eureka y monitoreo.
         */
        @Bean
        @Order(2)
        SecurityFilterChain actuatorSecurityChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/actuator/health", "/actuator/info")
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

                return http.build();
        }

        /**
         * Todo lo que no esté declarado explícitamente queda bloqueado.
         * La emisión OAuth2 corresponde exclusivamente a auth-server.
         */
        @Bean
        @Order(3)
        SecurityFilterChain denyByDefaultChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth.anyRequest().denyAll());

                return http.build();
        }
}
