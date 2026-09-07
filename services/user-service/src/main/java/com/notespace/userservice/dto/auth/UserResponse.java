package com.notespace.userservice.dto.auth;

import java.util.UUID;

public record UserResponse (
        UUID id,
        String username,
        String email
) {}
