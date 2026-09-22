package com.notespace.userservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {
    @Test
    void builderUsesUserRoleByDefault() {
        assertEquals(Role.USER, User.builder().build().getRole());
    }

    @Test
    void prePersistInitializesCreatedAndUpdatedTimestamps() {
        User user = new User();

        user.onCreate();

        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertFalse(user.getCreatedAt().isAfter(user.getUpdatedAt()));
    }

    @Test
    void preUpdateChangesUpdatedTimestampWithoutChangingCreationTimestamp() {
        User user = new User();
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime previousUpdate = LocalDateTime.of(2025, 1, 2, 0, 0);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(previousUpdate);

        user.onUpdate();

        assertEquals(createdAt, user.getCreatedAt());
        assertTrue(user.getUpdatedAt().isAfter(previousUpdate));
    }
}
