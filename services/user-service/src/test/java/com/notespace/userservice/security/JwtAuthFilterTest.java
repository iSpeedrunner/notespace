package com.notespace.userservice.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {
    @Mock JwtService jwtService;
    @Mock CustomUserDetailsService userDetailsService;
    @InjectMocks JwtAuthFilter filter;

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void filter_passesThroughWithoutAuthorizationHeader() throws ServletException, IOException {
        var chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void filter_passesThroughInvalidBearerToken() throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid");
        when(jwtService.isTokenValid("invalid")).thenReturn(false);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).isTokenValid("invalid");
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void filter_setsAuthenticationForValidBearerToken() throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid");
        var details = User.withUsername("alice@example.com").password("hash").roles("USER").build();
        when(jwtService.isTokenValid("valid")).thenReturn(true);
        when(jwtService.getEmailFromToken("valid")).thenReturn("alice@example.com");
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(details);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertEquals("alice@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
    }
}
