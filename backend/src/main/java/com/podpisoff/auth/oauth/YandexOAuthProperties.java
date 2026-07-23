package com.podpisoff.auth.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth.yandex")
public record YandexOAuthProperties(
    String clientId,
    String clientSecret,
    String redirectUri
) {
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
            && clientSecret != null && !clientSecret.isBlank()
            && redirectUri != null && !redirectUri.isBlank();
    }
}
