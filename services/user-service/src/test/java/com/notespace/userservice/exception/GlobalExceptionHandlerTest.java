package com.notespace.userservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

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

    @Test
    void duplicateUsername_isConflictWithStableErrorCode() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/register");

        var result = handler.handleUsernameAlreadyExists(new UsernameAlreadyExistsException(), request);

        assertEquals(409, result.getStatusCode().value());
        assertEquals("USERNAME_ALREADY_EXISTS", result.getBody().error());
        assertEquals("Username is already taken", result.getBody().message());
    }

    @Test
    void validationErrors_areMappedByFieldAndKeepFirstMessageForDuplicates() {
        BindingResult bindingResult = org.mockito.Mockito.mock(BindingResult.class);
        MethodArgumentNotValidException exception = org.mockito.Mockito.mock(MethodArgumentNotValidException.class);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/register");
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(
                new FieldError("request", "email", "required"),
                new FieldError("request", "email", "format"),
                new FieldError("request", "username", "invalid")
        ));

        var result = handler.handleValidation(exception, request);

        assertEquals(400, result.getStatusCode().value());
        assertEquals("VALIDATION_ERROR", result.getBody().error());
        assertEquals("required", result.getBody().fields().get("email"));
        assertEquals("invalid", result.getBody().fields().get("username"));
        assertEquals("/api/v1/auth/register", result.getBody().path());
    }
}
