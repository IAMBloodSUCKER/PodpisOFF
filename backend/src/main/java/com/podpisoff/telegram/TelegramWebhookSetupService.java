package com.podpisoff.telegram;

import com.podpisoff.settings.TelegramProperties;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TelegramWebhookSetupService {

    private final TelegramProperties telegramProperties;
    private final RestClient restClient;

    public TelegramWebhookSetupService(TelegramProperties telegramProperties) {
        this.telegramProperties = telegramProperties;
        this.restClient = RestClient.create();
    }

    public void registerWebhookIfConfigured() {
        if (!telegramProperties.isConfigured()) {
            return;
        }
        String publicUrl = telegramProperties.webhookPublicUrl();
        if (publicUrl == null || publicUrl.isBlank()) {
            return;
        }
        String webhookUrl = publicUrl.replaceAll("/$", "") + "/api/telegram/webhook";
        String apiUrl = "https://api.telegram.org/bot" + telegramProperties.botToken() + "/setWebhook";
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("url", webhookUrl);
        if (telegramProperties.webhookSecret() != null && !telegramProperties.webhookSecret().isBlank()) {
            body.put("secret_token", telegramProperties.webhookSecret());
        }
        restClient.post().uri(apiUrl).body(body).retrieve().toBodilessEntity();
    }
}
