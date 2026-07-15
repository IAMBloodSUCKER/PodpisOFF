package com.podpisoff.telegram;

import com.podpisoff.settings.TelegramProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TelegramWebhookSetupService {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookSetupService.class);

    private final TelegramProperties telegramProperties;
    private final TelegramBotApiClient telegramBotApiClient;

    public TelegramWebhookSetupService(TelegramProperties telegramProperties,
                                       TelegramBotApiClient telegramBotApiClient) {
        this.telegramProperties = telegramProperties;
        this.telegramBotApiClient = telegramBotApiClient;
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
        try {
            telegramBotApiClient.setWebhook(webhookUrl, telegramProperties.webhookSecret());
        } catch (Exception ex) {
            log.warn("Failed to register Telegram webhook: {}", safeError(ex));
        }
    }

    private String safeError(Throwable error) {
        return TelegramSecrets.safeErrorMessage(error, telegramBotApiClient.tokenForLogging());
    }
}
