package com.podpisoff.admin;

import java.time.Instant;
import java.time.LocalDateTime;

public record AdminUserResponse(
    Long id,
    String username,
    String email,
    String plan,
    String effectivePlan,
    LocalDateTime planExpiresAt,
    Instant createdAt,
    long subscriptionCount,
    long reminderCount,
    boolean blockedPermanently,
    LocalDateTime blockedUntil,
    boolean currentlyBlocked
) {
}
