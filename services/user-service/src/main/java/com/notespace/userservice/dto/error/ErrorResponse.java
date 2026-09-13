package com.notespace.userservice.dto.error;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse (
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fields
){}
