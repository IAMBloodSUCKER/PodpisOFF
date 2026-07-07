package com.podpisoff.auth;

final class AuthCredentialsValidator {

    private AuthCredentialsValidator() {
    }

    static boolean passwordMatchesUsername(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        return username.trim().equalsIgnoreCase(password.trim());
    }
}
