package com.flowboard.gateway.config;

import com.flowboard.gateway.filter.AuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final AuthFilter authFilter;

    /**
     * Programmatic route definitions.
     * These mirror the YAML routes in application.yml exactly.
     * YAML routes take priority — these serve as a fallback
     * and make it easy to add conditional routing logic later.
     *
     * If you want only YAML routes, you can delete this bean.
     * Both will not conflict — Spring Cloud Gateway deduplicates by route ID.
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // AUTH — public
                .route("auth-public-code", r -> r
                        .path("/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/validate")
                        .uri("http://localhost:8081"))

                // AUTH — admin (PLATFORM_ADMIN only)
                .route("auth-admin-code", r -> r
                        .path("/api/v1/auth/admin/**")
                        .filters(f -> f.filter(
                                authFilter.apply(new AuthFilter.Config("PLATFORM_ADMIN"))))
                        .uri("http://localhost:8081"))

                // AUTH — protected
                .route("auth-protected-code", r -> r
                        .path("/api/v1/auth/**")
                        .filters(f -> f.filter(
                                authFilter.apply(new AuthFilter.Config(null))))
                        .uri("http://localhost:8081"))

                // PUBLIC board view
                .route("public-boards-code", r -> r
                        .path("/api/v1/boards/{boardId}")
                        .and().method("GET")
                        .uri("http://localhost:8082"))

                // WORKSPACE-BOARD
                .route("workspace-board-code", r -> r
                        .path("/api/v1/workspaces/**", "/api/v1/boards/**")
                        .filters(f -> f.filter(
                                authFilter.apply(new AuthFilter.Config(null))))
                        .uri("http://localhost:8082"))

                // TASK
                .route("task-code", r -> r
                        .path("/api/v1/lists/**", "/api/v1/cards/**")
                        .filters(f -> f.filter(
                                authFilter.apply(new AuthFilter.Config(null))))
                        .uri("http://localhost:8083"))

                // COLLABORATION
                .route("collaboration-code", r -> r
                        .path("/api/v1/comments/**",
                                "/api/v1/attachments/**",
                                "/api/v1/labels/**",
                                "/api/v1/checklists/**",
                                "/api/v1/checklist-items/**")
                        .filters(f -> f.filter(
                                authFilter.apply(new AuthFilter.Config(null))))
                        .uri("http://localhost:8084"))

                // NOTIFICATION
                .route("notification-code", r -> r
                        .path("/api/v1/notifications/**")
                        .filters(f -> f.filter(
                                authFilter.apply(new AuthFilter.Config(null))))
                        .uri("http://localhost:8085"))

                .build();
    }
}