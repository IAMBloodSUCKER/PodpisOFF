package com.podpisoff.auth;

public record RegisterResponse(
    AuthResponse auth,
    String recoveryKey
) {
}
