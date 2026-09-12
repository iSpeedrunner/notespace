package com.notespace.userservice.service;

import com.notespace.userservice.dto.auth.RefreshTokenResult;
import com.notespace.userservice.entity.RefreshToken;
import com.notespace.userservice.entity.User;
import com.notespace.userservice.repository.AuthRepository;
import com.notespace.userservice.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {
    @Value("${jwt.refreshExpirationMs}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthRepository authRepository;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            AuthRepository authRepository
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authRepository = authRepository;
    }


    public RefreshTokenResult createRefreshToken(UUID userId) {

        User user = authRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String rawToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().plusMillis(refreshTokenDurationMs))
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);

        return new RefreshTokenResult(savedToken, rawToken);
    }

    public RefreshToken verifyToken(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new RuntimeException("Refresh token not found"));

        if(refreshToken.isRevoked()) {
            throw new RuntimeException("Refresh token is revoked.");
        }

        if(refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        return refreshToken;
    }


    public void revokeToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(token)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        refreshToken.setRevoked(true);

        refreshTokenRepository.save(refreshToken);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(token.getBytes(
                            StandardCharsets.UTF_8
                    ));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}