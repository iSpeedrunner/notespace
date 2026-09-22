package com.notespace.userservice.controller;

import com.notespace.userservice.dto.auth.UserResponse;
import com.notespace.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock UserService userService;
    @Mock Authentication authentication;
    @InjectMocks UserController controller;

    @Test
    void getCurrentUser_usesAuthenticatedName() {
        var response = new UserResponse(null, "alice", "alice@example.com");
        when(authentication.getName()).thenReturn("alice@example.com");
        when(userService.getCurrentUser("alice@example.com")).thenReturn(response);

        var result = controller.getCurrentUser(authentication);

        assertEquals(response, result.getBody());
        verify(userService).getCurrentUser("alice@example.com");
    }
}
