package com.notespace.workspace_service.exception;

import com.notespace.workspace_service.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.ErrorResponse;
import java.time.Instant;
import java.util.LinkedHashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(WorkspaceNotFoundException.class)
    ResponseEntity<ApiError> notFound(HttpServletRequest request) {
        return error(404, "WORKSPACE_NOT_FOUND", "Workspace not found", request);
    }

    @ExceptionHandler(CurrentUserRequiredException.class)
    ResponseEntity<ApiError> unauthorized(HttpServletRequest request) {
        return error(401, "CURRENT_USER_REQUIRED", "Trusted current user identity is required", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        var fields = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(field -> fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "VALIDATION_ERROR",
                "Invalid request data", request.getRequestURI(), fields));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalidName(HttpServletRequest request) {
        return error(400, "VALIDATION_ERROR", "Invalid request data", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> invalidUuid(HttpServletRequest request) {
        return error(400, "INVALID_UUID", "Invalid workspace ID", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformedJson(HttpServletRequest request) {
        return error(400, "MALFORMED_JSON", "Malformed request body", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        if (exception instanceof ErrorResponse response) {
            return error(response.getStatusCode().value(), "HTTP_ERROR", "Request could not be processed", request);
        }
        log.error("Workspace request failed", exception);
        return error(500, "INTERNAL_ERROR", "An internal error occurred", request);
    }

    private ResponseEntity<ApiError> error(int status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(status, code, message, request.getRequestURI()));
    }
}
