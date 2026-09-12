package com.notespace.userservice.repository;

import com.notespace.userservice.entity.RefreshToken;
import com.notespace.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String token);

    void deleteAllByUser(User user);
}
