package com.notespace.userservice.service;

import com.notespace.userservice.dto.auth.UserResponse;
import com.notespace.userservice.entity.User;
import com.notespace.userservice.repository.AuthRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
    private final AuthRepository authRepository;

    public UserResponse getCurrentUser(String email){
        User user = authRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Користувача не знайдено"));

        return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
    }
}
