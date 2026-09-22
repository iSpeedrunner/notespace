package com.notespace.userservice.service;

import com.notespace.userservice.entity.User;
import com.notespace.userservice.repository.AuthRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock AuthRepository authRepository;
    @InjectMocks UserService userService;

    @Test
    void getCurrentUser_returnsMappedResponse() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).username("alice").email("alice@example.com").build();
        when(authRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        var response = userService.getCurrentUser(user.getEmail());

        assertEquals(id, response.id());
        assertEquals("alice", response.username());
        assertEquals("alice@example.com", response.email());
    }

    @Test
    void getCurrentUser_throwsWhenUserDoesNotExist() {
        when(authRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userService.getCurrentUser("missing@example.com"));
    }
}
