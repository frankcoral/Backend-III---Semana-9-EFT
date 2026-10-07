package com.bancoxyz.bffmobile.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Bean
        @Order(1)
        SecurityFilterChain apiSecurityChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/**")
                                .csrf(AbstractHttpConfigurer::disable)
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/mobile/**")
                                                .hasAllAuthorities("SCOPE_banco.read", "SCOPE_canal.mobile")
                                                .anyRequest().denyAll())
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

                return http.build();
        }

        @Bean
        @Order(2)
        SecurityFilterChain actuatorSecurityChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/actuator/health", "/actuator/info")
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

                return http.build();
        }

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
