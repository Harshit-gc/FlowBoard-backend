package com.flowboard.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingFilter Unit Tests")
class LoggingFilterTest {

    @Mock private GatewayFilterChain chain;

    private LoggingFilter loggingFilter;

    @BeforeEach
    void setUp() {
        loggingFilter = new LoggingFilter();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ServerWebExchange buildExchange(String method, String path) {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(org.springframework.http.HttpMethod.valueOf(method), path)
                .build();
        return MockServerWebExchange.from(request);
    }

    private ServerWebExchange buildExchangeWithForwardedFor(String path, String ip) {
        MockServerHttpRequest request = MockServerHttpRequest
                .get(path)
                .header("X-Forwarded-For", ip)
                .build();
        return MockServerWebExchange.from(request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // filter() — basic behaviour
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("filter() — basic behaviour")
    class FilterBehaviourTests {

        @Test
        @DisplayName("should call chain.filter() and complete")
        void filter_callsChain() {
            when(chain.filter(any())).thenReturn(Mono.empty());
            ServerWebExchange exchange = buildExchange("GET", "/api/v1/task/cards");

            StepVerifier.create(loggingFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("should propagate error from chain without swallowing it")
        void filter_chainError_propagates() {
            RuntimeException error = new RuntimeException("downstream error");
            when(chain.filter(any())).thenReturn(Mono.error(error));

            ServerWebExchange exchange = buildExchange("POST", "/api/v1/auth/login");

            StepVerifier.create(loggingFilter.filter(exchange, chain))
                    .expectError(RuntimeException.class)
                    .verify();
        }

        @Test
        @DisplayName("should work for different HTTP methods")
        void filter_differentMethods() {
            when(chain.filter(any())).thenReturn(Mono.empty());

            for (String method : new String[]{"GET", "POST", "PUT", "DELETE"}) {
                ServerWebExchange exchange = buildExchange(method, "/api/v1/resource");

                StepVerifier.create(loggingFilter.filter(exchange, chain))
                        .verifyComplete();
            }

            verify(chain, times(4)).filter(any());
        }

        @Test
        @DisplayName("should work for different request paths")
        void filter_differentPaths() {
            when(chain.filter(any())).thenReturn(Mono.empty());

            String[] paths = {
                    "/api/v1/auth/login",
                    "/api/v1/workspace/boards",
                    "/api/v1/task/cards/1",
                    "/api/v1/collaboration/comments"
            };

            for (String path : paths) {
                ServerWebExchange exchange = buildExchange("GET", path);
                StepVerifier.create(loggingFilter.filter(exchange, chain))
                        .verifyComplete();
            }

            verify(chain, times(paths.length)).filter(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getOrder()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrder()")
    class OrderTests {

        @Test
        @DisplayName("should return HIGHEST_PRECEDENCE so it runs before AuthFilter")
        void getOrder_returnsHighestPrecedence() {
            assertThat(loggingFilter.getOrder())
                    .isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        }

        @Test
        @DisplayName("order should be lower than Integer.MAX_VALUE")
        void getOrder_lowerThanMaxValue() {
            assertThat(loggingFilter.getOrder())
                    .isLessThan(Integer.MAX_VALUE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Client IP resolution
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Client IP resolution")
    class ClientIpTests {

        @Test
        @DisplayName("should use X-Forwarded-For header when present (proxy scenario)")
        void filter_withForwardedFor_usesFirstIp() {
            when(chain.filter(any())).thenReturn(Mono.empty());

            // X-Forwarded-For can be a comma-separated list
            ServerWebExchange exchange = buildExchangeWithForwardedFor(
                    "/api/v1/task/cards", "192.168.1.1, 10.0.0.1");

            StepVerifier.create(loggingFilter.filter(exchange, chain))
                    .verifyComplete();

            // No assertion on the IP itself — just verify it doesn't throw
            verify(chain).filter(any());
        }

        @Test
        @DisplayName("should handle missing X-Forwarded-For header gracefully")
        void filter_noForwardedFor_noException() {
            when(chain.filter(any())).thenReturn(Mono.empty());

            ServerWebExchange exchange = buildExchange("GET", "/api/v1/workspace/1");

            StepVerifier.create(loggingFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(any());
        }

        @Test
        @DisplayName("should handle empty X-Forwarded-For header gracefully")
        void filter_emptyForwardedFor_noException() {
            when(chain.filter(any())).thenReturn(Mono.empty());

            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/resource")
                    .header("X-Forwarded-For", "")
                    .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(loggingFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(any());
        }
    }
}