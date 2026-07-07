package com.podpisoff.auth;

import com.podpisoff.user.LocaleCode;
import com.podpisoff.user.Plan;
import java.time.Instant;
import java.time.LocalDateTime;

public record AuthResponse(
    String token,
    Long id,
    String username,
    String email,
    Plan plan,
    LocalDateTime planExpiresAt,
    String timezone,
    LocaleCode locale,
    boolean termsAccepted,
    int billingReminderDaysBefore,
    boolean emailNotificationsEnabled,
    boolean telegramNotificationsEnabled,
    String telegramChatId,
    Instant createdAt
) {
}
