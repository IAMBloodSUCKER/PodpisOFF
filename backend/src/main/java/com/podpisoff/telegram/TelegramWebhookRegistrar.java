package com.podpisoff.telegram;

import com.podpisoff.settings.TelegramProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TelegramWebhookRegistrar implements ApplicationRunner {

    private final TelegramWebhookSetupService telegramWebhookSetupService;

    public TelegramWebhookRegistrar(TelegramWebhookSetupService telegramWebhookSetupService) {
        this.telegramWebhookSetupService = telegramWebhookSetupService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            telegramWebhookSetupService.registerWebhookIfConfigured();
        } catch (Exception ignored) {
            // Webhook registration is optional during local development.
        }
    }
}
