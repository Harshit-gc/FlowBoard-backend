package com.flowboard.gateway.filter;

import com.flowboard.gateway.config.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthFilter Unit Tests")
class AuthFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private GatewayFilterChain chain;

    private AuthFilter authFilter;
    private GatewayFilter filter;

    private static final String VALID_TOKEN   = "valid.jwt.token";
    private static final String INVALID_TOKEN = "invalid.jwt.token";

    @BeforeEach
    void setUp() {
        authFilter = new AuthFilter(jwtUtil);
        // Config with no required role — any authenticated user passes
        filter = authFilter.apply(new AuthFilter.Config());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ServerWebExchange exchangeWithBearer(String token) {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/task/cards")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        return MockServerWebExchange.from(request);
    }

    private ServerWebExchange exchangeWithNoAuth() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/task/cards")
                .build();
        return MockServerWebExchange.from(request);
    }

    private ServerWebExchange exchangeWithAuthHeader(String headerValue) {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/task/cards")
                .header(HttpHeaders.AUTHORIZATION, headerValue)
                .build();
        return MockServerWebExchange.from(request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Missing / malformed Authorization header
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Missing or malformed Authorization header")
    class AuthHeaderTests {

        @Test
        @DisplayName("should return 401 when Authorization header is missing")
        void missingAuthHeader_returns401() {
            ServerWebExchange exchange = exchangeWithNoAuth();

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should return 401 when Authorization header does not start with Bearer")
        void nonBearerAuthHeader_returns401() {
            ServerWebExchange exchange = exchangeWithAuthHeader("Basic dXNlcjpwYXNz");

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should return 401 when Authorization header value is just 'Bearer '")
        void emptyBearerToken_returns401() {
            when(jwtUtil.isTokenValid("")).thenReturn(false);
            ServerWebExchange exchange = exchangeWithAuthHeader("Bearer ");

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Token validation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Token validation")
    class TokenValidationTests {

        @Test
        @DisplayName("should return 401 when token is invalid or expired")
        void invalidToken_returns401() {
            when(jwtUtil.isTokenValid(INVALID_TOKEN)).thenReturn(false);
            ServerWebExchange exchange = exchangeWithBearer(INVALID_TOKEN);

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should forward request when token is valid")
        void validToken_forwardsRequest() {
            when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtUtil.getEmailFromToken(VALID_TOKEN)).thenReturn("user@example.com");
            when(jwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn("MEMBER");
            when(jwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(1);
            when(chain.filter(any())).thenReturn(Mono.empty());

            ServerWebExchange exchange = exchangeWithBearer(VALID_TOKEN);

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(any());
            assertThat(exchange.getResponse().getStatusCode())
                    .isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Downstream header enrichment
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Downstream header enrichment")
    class HeaderEnrichmentTests {

        @Test
        @DisplayName("should add X-User-Id, X-User-Email, X-User-Role to forwarded request")
        void validToken_addsEnrichmentHeaders() {
            when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtUtil.getEmailFromToken(VALID_TOKEN)).thenReturn("john@example.com");
            when(jwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn("BOARD_OWNER");
            when(jwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(42);

            // Capture the mutated exchange passed to chain
            final ServerWebExchange[] capturedExchange = new ServerWebExchange[1];
            when(chain.filter(any())).thenAnswer(inv -> {
                capturedExchange[0] = inv.getArgument(0);
                return Mono.empty();
            });

            ServerWebExchange exchange = exchangeWithBearer(VALID_TOKEN);

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            HttpHeaders headers = capturedExchange[0].getRequest().getHeaders();
            assertThat(headers.getFirst("X-User-Id")).isEqualTo("42");
            assertThat(headers.getFirst("X-User-Email")).isEqualTo("john@example.com");
            assertThat(headers.getFirst("X-User-Role")).isEqualTo("BOARD_OWNER");
        }

        @Test
        @DisplayName("should forward userId as string in X-User-Id header")
        void validToken_userIdConvertedToString() {
            when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtUtil.getEmailFromToken(VALID_TOKEN)).thenReturn("user@example.com");
            when(jwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn("MEMBER");
            when(jwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(99);

            final ServerWebExchange[] capturedExchange = new ServerWebExchange[1];
            when(chain.filter(any())).thenAnswer(inv -> {
                capturedExchange[0] = inv.getArgument(0);
                return Mono.empty();
            });

            StepVerifier.create(
                            filter.filter(exchangeWithBearer(VALID_TOKEN), chain))
                    .verifyComplete();

            assertThat(capturedExchange[0].getRequest()
                    .getHeaders().getFirst("X-User-Id")).isEqualTo("99");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Role-based access control
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Role-based access control")
    class RoleCheckTests {

        @Test
        @DisplayName("should return 403 when user role does not match requiredRole")
        void insufficientRole_returns403() {
            // Apply a filter that requires PLATFORM_ADMIN
            GatewayFilter adminFilter = authFilter.apply(
                    new AuthFilter.Config("PLATFORM_ADMIN"));

            when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtUtil.getEmailFromToken(VALID_TOKEN)).thenReturn("user@example.com");
            when(jwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn("MEMBER"); // not admin
            when(jwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(1);

            ServerWebExchange exchange = exchangeWithBearer(VALID_TOKEN);

            StepVerifier.create(adminFilter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should forward when user role exactly matches requiredRole")
        void matchingRole_forwardsRequest() {
            GatewayFilter adminFilter = authFilter.apply(
                    new AuthFilter.Config("PLATFORM_ADMIN"));

            when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtUtil.getEmailFromToken(VALID_TOKEN)).thenReturn("admin@example.com");
            when(jwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn("PLATFORM_ADMIN");
            when(jwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(2);
            when(chain.filter(any())).thenReturn(Mono.empty());

            ServerWebExchange exchange = exchangeWithBearer(VALID_TOKEN);

            StepVerifier.create(adminFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(any());
        }

        @Test
        @DisplayName("should forward any role when requiredRole is null (open to all auth users)")
        void nullRequiredRole_allowsAnyRole() {
            // Config with null requiredRole — default filter set in setUp()
            when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtUtil.getEmailFromToken(VALID_TOKEN)).thenReturn("user@example.com");
            when(jwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn("MEMBER");
            when(jwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(1);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(
                            filter.filter(exchangeWithBearer(VALID_TOKEN), chain))
                    .verifyComplete();

            verify(chain).filter(any());
        }

        @Test
        @DisplayName("should forward any role when requiredRole is empty string")
        void emptyRequiredRole_allowsAnyRole() {
            GatewayFilter openFilter = authFilter.apply(
                    new AuthFilter.Config("")); // empty string

            when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtUtil.getEmailFromToken(VALID_TOKEN)).thenReturn("user@example.com");
            when(jwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn("MEMBER");
            when(jwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(1);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(
                            openFilter.filter(exchangeWithBearer(VALID_TOKEN), chain))
                    .verifyComplete();

            verify(chain).filter(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AuthFilter.Config
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AuthFilter.Config")
    class ConfigTests {

        @Test
        @DisplayName("default Config should have null requiredRole")
        void defaultConfig_nullRequiredRole() {
            AuthFilter.Config config = new AuthFilter.Config();
            assertThat(config.getRequiredRole()).isNull();
        }

        @Test
        @DisplayName("parametrized Config should set requiredRole correctly")
        void parametrizedConfig_setsRequiredRole() {
            AuthFilter.Config config = new AuthFilter.Config("PLATFORM_ADMIN");
            assertThat(config.getRequiredRole()).isEqualTo("PLATFORM_ADMIN");
        }

        @Test
        @DisplayName("setRequiredRole should update the role")
        void setRequiredRole_updatesRole() {
            AuthFilter.Config config = new AuthFilter.Config();
            config.setRequiredRole("BOARD_OWNER");
            assertThat(config.getRequiredRole()).isEqualTo("BOARD_OWNER");
        }
    }
}