package com.notespace.userservice.controller;

import com.notespace.userservice.dto.auth.*;
import com.notespace.userservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock AuthService authService;
    @InjectMocks AuthController controller;

    @Test
    void register_returnsCreated() {
        var request = new RegisterRequest("alice", "alice@example.com", "password");
        var response = new UserResponse(null, "alice", "alice@example.com");
        when(authService.register(request)).thenReturn(response);

        var result = controller.register(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void login_returnsOk() {
        var request = new LoginRequest("alice@example.com", "password");
        var response = new LoginResponse("access", "refresh");
        when(authService.login(request)).thenReturn(response);

        var result = controller.login(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void logout_returnsNoContentAndDelegates() {
        var request = new LogoutRequest("refresh");

        var result = controller.logout(request);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(authService).logout("refresh");
    }

    @Test
    void refresh_returnsOk() {
        var request = new RefreshTokenRequest("refresh");
        var response = new LoginResponse("access", "new-refresh");
        when(authService.refreshToken(request)).thenReturn(response);

        var result = controller.refresh(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }
}
