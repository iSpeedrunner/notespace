package com.notespace.userservice.dto.error;

import java.time.LocalDateTime;

public record ErrorResponse (
        int status,
        String message,
        LocalDateTime timestamp
){}
