package com.notespace.userservice.service;

import com.notespace.userservice.dto.auth.RegisterRequest;
import com.notespace.userservice.dto.auth.UserResponse;
import com.notespace.userservice.entity.User;
import com.notespace.userservice.exception.EmailAlreadyExistsException;
import com.notespace.userservice.exception.UsernameAlreadyExistsException;
import com.notespace.userservice.repository.AuthRepository;
import com.notespace.userservice.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private AuthRepository authRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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

}
