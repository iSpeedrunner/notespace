package com.notespace.userservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
}
