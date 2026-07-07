package com.podpisoff.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthCredentialsValidatorTest {

    @Test
    void detectsMatchingUsernameAndPasswordCaseInsensitive() {
        assertTrue(AuthCredentialsValidator.passwordMatchesUsername("Admin123", "admin123"));
        assertTrue(AuthCredentialsValidator.passwordMatchesUsername(" user ", "user"));
    }

    @Test
    void allowsDifferentUsernameAndPassword() {
        assertFalse(AuthCredentialsValidator.passwordMatchesUsername("admin123", "password123"));
    }
}
