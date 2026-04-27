package com.flowboard.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();

        String requestId = request.getId();
        String method    = request.getMethod().name();
        String path      = request.getURI().getPath();
        String clientIp  = getClientIp(request);
        long   startTime = System.currentTimeMillis();

        log.info("[{}] --> {} {} from {}",
                requestId, method, path, clientIp);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            long duration = System.currentTimeMillis() - startTime;

            log.info("[{}] <-- {} {} | status={} | {}ms",
                    requestId, method, path,
                    response.getStatusCode(),
                    duration);
        }));
    }

    @Override
    public int getOrder() {
        // Runs first — before AuthFilter
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String getClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders()
                .getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress()
                    .getAddress().getHostAddress();
        }
        return "unknown";
    }
}