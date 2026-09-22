package com.notespace.userservice.service;

import com.notespace.userservice.dto.auth.RefreshTokenResult;
import com.notespace.userservice.entity.RefreshToken;
import com.notespace.userservice.entity.User;
import com.notespace.userservice.exception.InvalidRefreshTokenException;
import com.notespace.userservice.repository.AuthRepository;
import com.notespace.userservice.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuthRepository authRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                refreshTokenService,
                "refreshTokenDurationMs",
                3600000L
        );
    }

    @Test
    void createRefreshToken_ShouldCreateToken_WhenUserExists() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .build();

        RefreshToken savedToken = RefreshToken.builder()
                .user(user)
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();

        when(authRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(savedToken);

        RefreshTokenResult result =
                refreshTokenService.createRefreshToken(userId);

        assertNotNull(result);
        assertNotNull(result.rawToken());
        assertEquals(savedToken, result.refreshToken());

        verify(authRepository).findById(userId);
        verify(refreshTokenRepository).save(any(RefreshToken.class));

        var captor = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertTrue(Pattern.matches("[0-9a-f]{64}", captor.getValue().getTokenHash()));
        assertNotEquals(result.rawToken(), captor.getValue().getTokenHash());
    }

    @Test
    void createRefreshToken_ShouldThrowException_WhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();

        when(authRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> refreshTokenService.createRefreshToken(userId)
        );

        verify(authRepository).findById(userId);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void createRefreshToken_shouldPropagateRepositoryException() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        RuntimeException failure = new IllegalStateException("database unavailable");
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> refreshTokenService.createRefreshToken(userId)));
    }

    @Test
    void createRefreshToken_shouldTreatNullUserIdAsMissingWhenRepositoryReturnsEmpty() {
        when(authRepository.findById(null)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> refreshTokenService.createRefreshToken(null));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rotateRefreshToken_ShouldCreateNewToken_WhenTokenIsValid() {
        String rawToken = "valid-token";
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .build();

        RefreshToken oldToken = RefreshToken.builder()
                .user(user)
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();

        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();

        RefreshTokenResult newTokenResult =
                new RefreshTokenResult(newToken, "new-raw-token");

        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(oldToken));

        when(refreshTokenRepository.save(oldToken))
                .thenReturn(oldToken);

        when(authRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(refreshTokenRepository.save(argThat(token ->
                token != oldToken
        ))).thenReturn(newToken);

        RefreshTokenResult result =
                refreshTokenService.rotateRefreshToken(rawToken);

        assertNotNull(result);
        assertEquals(newToken, result.refreshToken());
        assertNotNull(result.rawToken());
        assertFalse(result.rawToken().isBlank());

        verify(refreshTokenRepository).findByTokenHash(anyString());
        verify(refreshTokenRepository).save(oldToken);
        verify(authRepository).findById(userId);
    }

    @Test
    void rotateRefreshToken_ShouldRevokeOldToken_WhenTokenIsValid() {
        String rawToken = "valid-token";
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .build();

        RefreshToken oldToken = RefreshToken.builder()
                .user(user)
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();

        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(oldToken));

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(newToken);

        when(authRepository.findById(userId))
                .thenReturn(Optional.of(user));

        refreshTokenService.rotateRefreshToken(rawToken);

        assertTrue(oldToken.isRevoked());

        verify(refreshTokenRepository).save(oldToken);
    }

    @Test
    void rotateRefreshToken_ShouldThrowException_WhenTokenDoesNotExist() {
        String rawToken = "unknown-token";

        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotateRefreshToken(rawToken)
        );

        verify(refreshTokenRepository).findByTokenHash(anyString());

        verify(refreshTokenRepository, never())
                .save(any());

        verify(authRepository, never())
                .findById(any());
    }

    @Test
    void rotateRefreshToken_ShouldThrowException_WhenTokenIsExpired() {
        String rawToken = "expired-token";

        User user = User.builder()
                .id(UUID.randomUUID())
                .build();

        RefreshToken expiredToken = RefreshToken.builder()
                .user(user)
                .revoked(false)
                .expiresAt(Instant.now().minusSeconds(3600))
                .createdAt(Instant.now().minusSeconds(7200))
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(expiredToken));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotateRefreshToken(rawToken)
        );

        verify(refreshTokenRepository).findByTokenHash(anyString());

        verify(refreshTokenRepository, never())
                .save(any());

        verify(authRepository, never())
                .findById(any());
    }

    @Test
    void rotateRefreshToken_ShouldThrowException_WhenTokenIsRevoked() {
        String rawToken = "revoked-token";

        User user = User.builder()
                .id(UUID.randomUUID())
                .build();

        RefreshToken revokedToken = RefreshToken.builder()
                .user(user)
                .revoked(true)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(revokedToken));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotateRefreshToken(rawToken)
        );

        verify(refreshTokenRepository).findByTokenHash(anyString());

        verify(refreshTokenRepository, never())
                .save(any());

        verify(authRepository, never())
                .findById(any());
    }

    @Test
    void rotateRefreshToken_shouldNotCreateReplacementWhenOldTokenUserIsMissing() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        RefreshToken oldToken = RefreshToken.builder()
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(oldToken)).thenReturn(oldToken);
        when(authRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> refreshTokenService.rotateRefreshToken("raw-token"));

        assertTrue(oldToken.isRevoked());
        verify(refreshTokenRepository).save(oldToken);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshToken_shouldPropagateFailureWhileRevokingOldToken() {
        RefreshToken oldToken = RefreshToken.builder()
                .user(User.builder().id(UUID.randomUUID()).build())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
        RuntimeException failure = new IllegalStateException("database unavailable");
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(oldToken)).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> refreshTokenService.rotateRefreshToken("raw-token")));
        verifyNoInteractions(authRepository);
    }

    @Test
    void rotateRefreshToken_shouldRejectNullRawTokenBeforeRepositoryLookup() {
        assertThrows(NullPointerException.class,
                () -> refreshTokenService.rotateRefreshToken(null));
        verifyNoInteractions(refreshTokenRepository, authRepository);
    }

    @Test
    void revokeToken_ShouldRevokeToken_WhenTokenIsValid() {
        String rawToken = "valid-token";

        RefreshToken refreshToken = RefreshToken.builder()
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(refreshToken));

        when(refreshTokenRepository.save(refreshToken))
                .thenReturn(refreshToken);

        refreshTokenService.revokeToken(rawToken);

        assertTrue(refreshToken.isRevoked());

        verify(refreshTokenRepository)
                .findByTokenHash(anyString());

        verify(refreshTokenRepository)
                .save(refreshToken);
    }

    @Test
    void revokeToken_ShouldThrowException_WhenTokenDoesNotExist() {
        String rawToken = "unknown-token";

        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.revokeToken(rawToken)
        );

        verify(refreshTokenRepository)
                .findByTokenHash(anyString());

        verify(refreshTokenRepository, never())
                .save(any());
    }

    @Test
    void revokeToken_shouldPropagateRepositorySaveFailure() {
        RefreshToken token = RefreshToken.builder().revoked(false).build();
        RuntimeException failure = new IllegalStateException("database unavailable");
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(token)).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> refreshTokenService.revokeToken("raw-token")));
        assertTrue(token.isRevoked());
    }

    @Test
    void revokeToken_shouldRejectNullTokenBeforeRepositoryLookup() {
        assertThrows(NullPointerException.class, () -> refreshTokenService.revokeToken(null));
        verifyNoInteractions(refreshTokenRepository);
    }
}
