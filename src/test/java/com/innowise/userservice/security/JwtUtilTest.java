package com.innowise.userservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    @Test
    void extractClaims_Success() {
        String token = Jwts.builder()
                .claim("email", "john.doe@example.com")
                .claim("userId", 42L)
                .claim("role", "USER")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();

        assertEquals("john.doe@example.com", jwtUtil.extractEmail(token));
        assertEquals(42L, jwtUtil.extractUserId(token));
        assertEquals("USER", jwtUtil.extractRole(token));
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_ExpiredToken_ReturnsFalse() {
        String token = Jwts.builder()
                .claim("email", "john.doe@example.com")
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_InvalidToken_ReturnsFalse() {
        String token = "not-a-jwt";

        assertFalse(jwtUtil.isTokenValid(token));
    }
}
