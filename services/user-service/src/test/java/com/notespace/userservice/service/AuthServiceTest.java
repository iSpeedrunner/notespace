package com.notespace.userservice.service;

import com.notespace.userservice.dto.auth.RegisterRequest;
import com.notespace.userservice.dto.auth.UserResponse;
import com.notespace.userservice.entity.User;
import com.notespace.userservice.repository.AuthRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
}
