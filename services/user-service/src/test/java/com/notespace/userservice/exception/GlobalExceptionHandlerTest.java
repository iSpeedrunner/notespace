package com.notespace.userservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {
    @Mock HttpServletRequest request;
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void duplicateEmail_isConflictWithStableErrorCode() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/register");

        var result = handler.handleEmailAlreadyExists(new EmailAlreadyExistsException(), request);

        assertEquals(409, result.getStatusCode().value());
        assertEquals("EMAIL_ALREADY_EXISTS", result.getBody().error());
        assertEquals("/api/v1/auth/register", result.getBody().path());
    }

    @Test
    void invalidCredentials_isUnauthorized() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        var result = handler.handleInvalidCredentials(new InvalidCredentialsException(), request);

        assertEquals(401, result.getStatusCode().value());
        assertEquals("BAD_CREDENTIALS", result.getBody().error());
    }

    @Test
    void invalidRefreshToken_isUnauthorized() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/refresh");

        var result = handler.handleInvalidRefreshToken(new InvalidRefreshTokenException(), request);

        assertEquals(401, result.getStatusCode().value());
        assertEquals("INVALID_REFRESH_TOKEN", result.getBody().error());
    }
}
