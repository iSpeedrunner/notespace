package com.notespace.userservice.service;

import com.notespace.userservice.dto.auth.*;
import com.notespace.userservice.entity.RefreshToken;
import com.notespace.userservice.entity.User;
import com.notespace.userservice.exception.EmailAlreadyExistsException;
import com.notespace.userservice.exception.InvalidCredentialsException;
import com.notespace.userservice.exception.UsernameAlreadyExistsException;
import com.notespace.userservice.repository.AuthRepository;
import com.notespace.userservice.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private AuthRepository authRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldReturnCreatedUser_whenEmailAndUsernameAreAvailable() {
        UUID id = UUID.randomUUID();
        RegisterRequest request = new RegisterRequest("hello11", "hello1@gmail.com", "helloWorld");

        User user = new User();
        user.setId(id);
        user.setUsername("hello11");
        user.setEmail("hello1@gmail.com");

        when(authRepository.existsByEmail(request.email()))
                .thenReturn(false);

        when(authRepository.existsByUsername(request.username()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.password()))
                .thenReturn("hashedPassword");

        when(authRepository.save(any(User.class)))
                .thenReturn(user);

        UserResponse response = authService.register(request);

        assertEquals(id, response.id());
        assertEquals("hello11", response.username());
        assertEquals("hello1@gmail.com", response.email());
    }

    @Test
    void register_shouldReturnException_whenEmailDoesAlreadyExists() {
        RegisterRequest request = new RegisterRequest("hello11", "hello1@gmail.com", "helloWorld");

        when(authRepository.existsByEmail(request.email()))
                .thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    void register_shouldReturnException_whenUsernameDoesAlreadyExists() {
        RegisterRequest request = new RegisterRequest("hello11", "hello1@gmail.com", "helloWorld");

        when(authRepository.existsByUsername(request.username()))
                .thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    void register_shouldSaveUser_whenDataIsValid() {
        RegisterRequest request = new RegisterRequest("hello11", "hello1@gmail.com", "helloWorld");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(request.password());

        when(authRepository.existsByUsername(request.username()))
                .thenReturn(false);

        when(authRepository.existsByEmail(request.email()))
                .thenReturn(false);

        when(authRepository.save(any(User.class)))
                .thenReturn(user);

        authService.register(request);

        verify(authRepository).save(any(User.class));
    }

    @Test
    void register_shouldEncodePassword() {
        RegisterRequest request = new RegisterRequest("hello11", "hello1@gmail.com", "helloWorld");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(request.username());
        user.setEmail(request.email());

        when(authRepository.existsByUsername(request.username()))
                .thenReturn(false);

        when(authRepository.existsByEmail(request.email()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.password()))
                .thenReturn("hashedPassword");

        when(authRepository.save(any(User.class)))
                .thenReturn(user);

        authService.register(request);

        verify(passwordEncoder).encode(request.password());
    }


    @Test
    void login_ShouldReturnLoginResponse_WhenCredentialsAreValid() {
        String email = "test@gmail.com";
        String password = "password123";
        UUID id = UUID.randomUUID();

        LoginRequest req = new LoginRequest(email, password);

        User user = User.builder()
                .id(id)
                .email(email)
                .build();

        RefreshTokenResult refreshTokenResult = new RefreshTokenResult(
                mock(RefreshToken.class),
                "refresh-token-raw"
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));

        when(authRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(email))
                .thenReturn("access-token");

        when(refreshTokenService.createRefreshToken(id))
                .thenReturn(refreshTokenResult);

        LoginResponse response = authService.login(req);

        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token-raw", response.refreshToken());

        verify(authenticationManager).authenticate(any());
        verify(authRepository).findByEmail(email);
        verify(jwtService).generateToken(email);
        verify(refreshTokenService).createRefreshToken(id);
    }

    @Test
    void login_shouldThrowInvalidCredentialsException_whenPasswordIsIncorrect() {
        LoginRequest request = new LoginRequest(
                "test@gmail.com",
                "wrong_password"
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );
        verifyNoInteractions(
                authRepository,
                jwtService,
                refreshTokenService
        );
    }

    @Test
    void login_shouldNormalizeEmail_beforeAuthentication() {

    }
}