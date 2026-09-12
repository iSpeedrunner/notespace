package com.notespace.userservice.service;

import com.notespace.userservice.entity.User;
import com.notespace.userservice.repository.AuthRepository;
import com.notespace.userservice.repository.RefreshTokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

//    public String createRefreshToken(User user) {
//
//    }
}