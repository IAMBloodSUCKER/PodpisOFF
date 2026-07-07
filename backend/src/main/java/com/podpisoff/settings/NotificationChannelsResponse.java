package com.podpisoff.settings;

public record NotificationChannelsResponse(
    boolean emailConfigured,
    boolean telegramConfigured,
    String telegramBotUsername
) {
}
