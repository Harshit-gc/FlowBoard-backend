package com.flowboard.gateway.filter;

import com.flowboard.gateway.config.JwtUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
@Slf4j
public class AuthFilter extends
        AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final JwtUtil jwtUtil;

    public AuthFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            String method = request.getMethod().name();

            log.debug("AuthFilter → {} {}", method, path);

            // ── Step 1: Authorization header must exist ──────────────────────
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("Missing Authorization header → {}", path);
                return onError(exchange,
                        "Authorization header is missing",
                        HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Malformed Authorization header → {}", path);
                return onError(exchange,
                        "Authorization header must start with Bearer",
                        HttpStatus.UNAUTHORIZED);
            }

            // ── Step 2: Validate JWT ─────────────────────────────────────────
            String token = authHeader.substring(7);

            if (!jwtUtil.isTokenValid(token)) {
                log.warn("Invalid or expired JWT → {}", path);
                return onError(exchange,
                        "Invalid or expired JWT token",
                        HttpStatus.UNAUTHORIZED);
            }

            // ── Step 3: Extract claims ───────────────────────────────────────
            String email = jwtUtil.getEmailFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);
            Integer userId = jwtUtil.getUserIdFromToken(token);

            log.debug("JWT valid → userId={} role={} path={}",
                    userId, role, path);

            // ── Step 4: Role check (only if requiredRole is set) ────────────
            if (config.getRequiredRole() != null
                    && !config.getRequiredRole().isEmpty()
                    && !role.equals(config.getRequiredRole())) {

                log.warn("Access denied → userId={} role={} requiredRole={} path={}",
                        userId, role, config.getRequiredRole(), path);

                return onError(exchange,
                        "Access denied — insufficient permissions",
                        HttpStatus.FORBIDDEN);
            }

            // ── Step 5: Forward enrichment headers to downstream services ────
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-User-Email", email)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(
                    exchange.mutate().request(mutatedRequest).build());
        };
    }

    // ── Short-circuit with JSON error response ───────────────────────────────
    private Mono<Void> onError(ServerWebExchange exchange,
                               String message,
                               HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"message\":\"%s\"}",
                LocalDateTime.now(), status.value(), message
        );

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    // ── Config class ─────────────────────────────────────────────────────────
    @Data
    public static class Config {
        // Set in YAML via args.requiredRole
        // Null means any authenticated user can pass
        private String requiredRole;

        // No-arg constructor needed by YAML filter parsing
        public Config() {
        }

        // Constructor used by GatewayConfig.java programmatic routes
        public Config(String requiredRole) {
            this.requiredRole = requiredRole;
        }
    }
}