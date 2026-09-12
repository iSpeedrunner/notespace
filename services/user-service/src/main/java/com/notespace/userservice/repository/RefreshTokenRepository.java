package com.notespace.userservice.repository;

import com.notespace.userservice.entity.RefreshToken;
import com.notespace.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query("""
            SELECT rt
            FROM RefreshToken rt
            JOIN FETCH rt.user
            WHERE rt.tokenHash = :tokenHash
           """)
    Optional<RefreshToken> findByTokenHash(@Param("tokenHash") String tokenHash);

}
