package com.flowboard.gateway.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET =
            "flowboard_super_secret_key_for_testing_must_be_256_bits_long_at_least";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    // ── Helper to generate test tokens ────────────────────────────────────────

    private String generateToken(String email, String role, Integer userId,
                                 long expiryMs) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        return Jwts.builder()
                .setSubject(email)
                .addClaims(Map.of("role", role, "userId", userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateExpiredToken(String email) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        return Jwts.builder()
                .setSubject(email)
                .addClaims(Map.of("role", "MEMBER", "userId", 1))
                .setIssuedAt(new Date(System.currentTimeMillis() - 10000))
                .setExpiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // isTokenValid()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValidTests {

        @Test
        @DisplayName("should return true for a freshly generated valid token")
        void isTokenValid_freshToken_returnsTrue() {
            String token = generateToken("user@example.com", "MEMBER", 1, 86400000L);
            assertThat(jwtUtil.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("should return false for an expired token")
        void isTokenValid_expiredToken_returnsFalse() {
            String token = generateExpiredToken("user@example.com");
            assertThat(jwtUtil.isTokenValid(token)).isFalse();
        }

        @Test
        @DisplayName("should return false for a garbage string")
        void isTokenValid_garbageString_returnsFalse() {
            assertThat(jwtUtil.isTokenValid("not.a.jwt")).isFalse();
        }

        @Test
        @DisplayName("should return false for an empty string")
        void isTokenValid_emptyString_returnsFalse() {
            assertThat(jwtUtil.isTokenValid("")).isFalse();
        }

        @Test
        @DisplayName("should return false for null")
        void isTokenValid_null_returnsFalse() {
            assertThat(jwtUtil.isTokenValid(null)).isFalse();
        }

        @Test
        @DisplayName("should return false for a token signed with a different secret")
        void isTokenValid_wrongSecret_returnsFalse() {
            Key wrongKey = Keys.hmacShaKeyFor(
                    "completely_different_secret_key_that_is_256_bits_at_least!".getBytes());
            String token = Jwts.builder()
                    .setSubject("user@example.com")
                    .setExpiration(new Date(System.currentTimeMillis() + 86400000L))
                    .signWith(wrongKey, SignatureAlgorithm.HS256)
                    .compact();

            assertThat(jwtUtil.isTokenValid(token)).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getEmailFromToken()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getEmailFromToken()")
    class GetEmailFromTokenTests {

        @Test
        @DisplayName("should return the email used as subject during generation")
        void getEmailFromToken_returnsCorrectEmail() {
            String token = generateToken("john@example.com", "MEMBER", 1, 86400000L);
            assertThat(jwtUtil.getEmailFromToken(token)).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should return different email for different tokens")
        void getEmailFromToken_differentSubjects() {
            String token1 = generateToken("alice@example.com", "MEMBER", 1, 86400000L);
            String token2 = generateToken("bob@example.com", "ADMIN", 2, 86400000L);

            assertThat(jwtUtil.getEmailFromToken(token1)).isEqualTo("alice@example.com");
            assertThat(jwtUtil.getEmailFromToken(token2)).isEqualTo("bob@example.com");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getRoleFromToken()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRoleFromToken()")
    class GetRoleFromTokenTests {

        @Test
        @DisplayName("should return MEMBER role from token")
        void getRoleFromToken_memberRole() {
            String token = generateToken("user@example.com", "MEMBER", 1, 86400000L);
            assertThat(jwtUtil.getRoleFromToken(token)).isEqualTo("MEMBER");
        }

        @Test
        @DisplayName("should return PLATFORM_ADMIN role from token")
        void getRoleFromToken_adminRole() {
            String token = generateToken("admin@example.com", "PLATFORM_ADMIN", 2, 86400000L);
            assertThat(jwtUtil.getRoleFromToken(token)).isEqualTo("PLATFORM_ADMIN");
        }

        @Test
        @DisplayName("should return BOARD_OWNER role from token")
        void getRoleFromToken_boardOwnerRole() {
            String token = generateToken("owner@example.com", "BOARD_OWNER", 3, 86400000L);
            assertThat(jwtUtil.getRoleFromToken(token)).isEqualTo("BOARD_OWNER");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getUserIdFromToken()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserIdFromToken()")
    class GetUserIdFromTokenTests {

        @Test
        @DisplayName("should return the userId embedded in the token")
        void getUserIdFromToken_returnsCorrectId() {
            String token = generateToken("user@example.com", "MEMBER", 42, 86400000L);
            assertThat(jwtUtil.getUserIdFromToken(token)).isEqualTo(42);
        }

        @Test
        @DisplayName("should return different userIds for different tokens")
        void getUserIdFromToken_differentIds() {
            String token1 = generateToken("a@example.com", "MEMBER", 10, 86400000L);
            String token2 = generateToken("b@example.com", "MEMBER", 99, 86400000L);

            assertThat(jwtUtil.getUserIdFromToken(token1)).isEqualTo(10);
            assertThat(jwtUtil.getUserIdFromToken(token2)).isEqualTo(99);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Combined claims validation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Combined claim extraction")
    class CombinedClaimsTests {

        @Test
        @DisplayName("all three claims should be independently correct in same token")
        void allClaims_correctInSameToken() {
            String token = generateToken("jane@example.com", "BOARD_OWNER", 7, 86400000L);

            assertThat(jwtUtil.getEmailFromToken(token)).isEqualTo("jane@example.com");
            assertThat(jwtUtil.getRoleFromToken(token)).isEqualTo("BOARD_OWNER");
            assertThat(jwtUtil.getUserIdFromToken(token)).isEqualTo(7);
            assertThat(jwtUtil.isTokenValid(token)).isTrue();
        }
    }
}