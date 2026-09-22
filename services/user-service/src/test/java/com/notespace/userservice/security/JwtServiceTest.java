package com.notespace.userservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {
    private static final String SECRET = "01234567890123456789012345678901";

    @InjectMocks JwtService jwtService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
    }

    @Test
    void generatedToken_containsEmailAndIsValid() {
        String token = jwtService.generateToken("alice@example.com");

        assertEquals("alice@example.com", jwtService.getEmailFromToken(token));
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void tokenWithDifferentSignatureIsInvalid() {
        JwtService other = new JwtService();
        ReflectionTestUtils.setField(other, "secret", "abcdefghijklmnopqrstuvwxyz123456");

        String token = jwtService.generateToken("alice@example.com");

        assertFalse(other.isTokenValid(token));
    }

    @Test
    void malformedTokenIsInvalid() {
        assertFalse(jwtService.isTokenValid("not-a-jwt"));
    }

    @Test
    void expiredTokenIsInvalid() {
        String token = Jwts.builder()
                .subject("alice@example.com")
                .expiration(new Date(0))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertFalse(jwtService.isTokenValid(token));
        assertThrows(Exception.class, () -> jwtService.getEmailFromToken(token));
    }

    @Test
    void getEmailFromToken_throwsForNullToken() {
        assertThrows(Exception.class, () -> jwtService.getEmailFromToken(null));
        assertFalse(jwtService.isTokenValid(null));
    }

    @Test
    void generateToken_rejectsSecretShorterThanHmacRequirement() {
        ReflectionTestUtils.setField(jwtService, "secret", "too-short");

        assertThrows(Exception.class, () -> jwtService.generateToken("alice@example.com"));
    }
}
