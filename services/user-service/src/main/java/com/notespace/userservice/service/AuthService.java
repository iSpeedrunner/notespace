package com.notespace.userservice.service;

import com.notespace.userservice.dto.auth.*;
import com.notespace.userservice.entity.RefreshToken;
import com.notespace.userservice.entity.Role;
import com.notespace.userservice.entity.User;
import com.notespace.userservice.exception.UserAlreadyExistsException;
import com.notespace.userservice.repository.AuthRepository;
import com.notespace.userservice.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public UserResponse register(RegisterRequest req) {
        if(authRepository.existsByEmail(req.email()))
            throw new UserAlreadyExistsException("Користувач з email: '" + req.email() + "' вже існує.");

        if(authRepository.existsByUsername(req.username()))
            throw new UserAlreadyExistsException("Ім'я користувача '" + req.username() + "' вже зайняте");

        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(Role.USER)
                .build();

        User savedUser = authRepository.save(user);

        return new UserResponse(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = authRepository.findByEmail(authentication.getName()).orElseThrow();

        String accessToken = jwtService.generateToken(authentication.getName());

        RefreshTokenResult refreshTokenResult = refreshTokenService.createRefreshToken(user.getId());

        return new LoginResponse(accessToken,
                refreshTokenResult.rawToken());
    }

    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }

    public LoginResponse refreshToken (RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyToken(request.refreshToken());

        User user = refreshToken.getUser();

        String accessToken = jwtService.generateToken(user.getEmail());

        return new LoginResponse(
                accessToken,
                request.refreshToken()
        );
    }
}
