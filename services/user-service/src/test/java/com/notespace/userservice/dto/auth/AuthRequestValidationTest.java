package com.notespace.userservice.dto.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthRequestValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validator = null;
    }

    @Test
    void register_acceptsDeclaredBoundaryLengthsAndValidCharacters() {
        String username = "a".repeat(50);
        String email = "a".repeat(63) + "@" + "b".repeat(63) + "."
                + "c".repeat(63) + "." + "d".repeat(61);
        String password = "p".repeat(72);

        assertTrue(validator.validate(new RegisterRequest(username, email, password)).isEmpty());
    }

    @Test
    void register_rejectsValuesOutsideDeclaredConstraints() {
        assertFieldsInvalid(new RegisterRequest("ab", "not-an-email", "short"),
                "username", "email", "password");
        assertFieldsInvalid(new RegisterRequest("bad-name", "alice@example.com", "password123"),
                "username");
        assertFieldsInvalid(new RegisterRequest("alice", "a".repeat(250), "password123"),
                "email");
        assertFieldsInvalid(new RegisterRequest("alice", "alice@example.com", "p".repeat(73)),
                "password");
    }

    @Test
    void register_rejectsNullAndBlankValues() {
        assertFieldsInvalid(new RegisterRequest(null, null, null), "username", "email", "password");
        assertFieldsInvalid(new RegisterRequest("   ", "   ", "   "), "username", "email", "password");
    }

    @Test
    void login_requiresValidEmailAndNonBlankPassword() {
        assertFieldsInvalid(new LoginRequest("invalid", ""), "email", "password");
        assertFieldsInvalid(new LoginRequest(null, null), "email", "password");
    }

    @Test
    void logoutAndRefreshRequireNonBlankRefreshToken() {
        assertFieldsInvalid(new LogoutRequest(null), "refreshToken");
        assertFieldsInvalid(new LogoutRequest(" "), "refreshToken");
        assertFieldsInvalid(new RefreshTokenRequest(""), "refreshToken");
    }

    private void assertFieldsInvalid(Object request, String... fields) {
        var violations = validator.validate(request);
        for (String field : fields) {
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(field)),
                    () -> "Expected validation failure for " + field);
        }
    }
}
