package com.notespace.userservice.exception;

import com.notespace.userservice.dto.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        Map<String, String> fields = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        DefaultMessageSourceResolvable::getDefaultMessage,
                        (existing, replacement) -> existing
                ));

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Request validation failed",
                request.getRequestURI(),
                fields
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex, HttpServletRequest request) {
        return buildError(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_EXISTS",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameAlreadyExists(
            UsernameAlreadyExistsException ex,
            HttpServletRequest request
    ) {

        return buildError(
                HttpStatus.CONFLICT,
                "USERNAME_ALREADY_EXISTS",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "BAD_CREDENTIALS",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest request) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "INVALID_REFRESH_TOKEN",
                ex.getMessage(),
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request
    ) {

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}