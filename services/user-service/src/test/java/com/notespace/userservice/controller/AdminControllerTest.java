package com.notespace.userservice.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminControllerTest {
    @Test
    void isAdmin_returnsGreeting() {
        var result = new AdminController().isAdmin();

        assertEquals(200, result.getStatusCode().value());
        assertEquals("Hello admin", result.getBody());
    }
}
