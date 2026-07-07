package com.podpisoff.settings;

import jakarta.validation.constraints.Size;

public record SettingsUpdateRequest(
    Integer billingReminderDaysBefore,
    Boolean emailNotificationsEnabled,
    Boolean telegramNotificationsEnabled,
    @Size(max = 32) String telegramChatId,
    @Size(max = 255) String email
) {
}
