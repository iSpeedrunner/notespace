package com.notespace.userservice.security;

import com.notespace.userservice.entity.Role;
import com.notespace.userservice.entity.User;
import com.notespace.userservice.repository.AuthRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    @Mock AuthRepository authRepository;
    @InjectMocks CustomUserDetailsService service;

    @Test
    void loadUserByUsername_mapsCredentialsAndRole() {
        User user = User.builder().email("admin@example.com").passwordHash("hash").role(Role.ADMIN).build();
        when(authRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername(user.getEmail());

        assertEquals("admin@example.com", details.getUsername());
        assertEquals("hash", details.getPassword());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_throwsWhenUserDoesNotExist() {
        when(authRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing@example.com"));
    }
}
