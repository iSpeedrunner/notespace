package com.notespace.workspace_service.config;

import com.notespace.workspace_service.dto.ApiError;
import com.notespace.workspace_service.exception.CurrentUserRequiredException;
import com.notespace.workspace_service.service.CurrentUserProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CurrentUserProvider users, ObjectMapper mapper) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable).requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> unauthorized(request, response, mapper)))
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                            throws ServletException, IOException {
                        UUID userId;
                        try {
                            userId = users.getCurrentUserId();
                            if (userId == null) throw new CurrentUserRequiredException();
                        } catch (CurrentUserRequiredException exception) {
                            unauthorized(request, response, mapper);
                            return;
                        }
                        var context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
                        SecurityContextHolder.setContext(context);
                        try {
                            chain.doFilter(request, response);
                        } finally {
                            SecurityContextHolder.clearContext();
                        }
                    }
                }, AnonymousAuthenticationFilter.class).build();
    }

    private static void unauthorized(HttpServletRequest request, HttpServletResponse response, ObjectMapper mapper) throws IOException {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(mapper.writeValueAsString(ApiError.of(401, "CURRENT_USER_REQUIRED",
                "Trusted current user identity is required", request.getRequestURI())));
    }
}
