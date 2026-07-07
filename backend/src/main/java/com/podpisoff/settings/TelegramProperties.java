package com.podpisoff.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.telegram")
public record TelegramProperties(
    String botToken,
    String botUsername,
    String webhookSecret,
    String webhookPublicUrl
) {
    public boolean isConfigured() {
        return botToken != null && !botToken.isBlank();
    }
}
