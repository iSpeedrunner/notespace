package com.notespace.userservice.dto.auth;

import com.notespace.userservice.entity.RefreshToken;

public record RefreshTokenResult(
    RefreshToken refreshToken,
    String rawToken
) {}