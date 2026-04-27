package com.flowboard.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class ActuatorSecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                        // Actuator endpoints — open without auth
                        .pathMatchers("/actuator/**").permitAll()
                        // Everything else — permit all
                        // AuthFilter handles JWT validation, not Spring Security
                        .anyExchange().permitAll()
                );
        return http.build();
    }
}