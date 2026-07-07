package com.podpisoff.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.podpisoff.settings.TelegramProperties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TelegramPollingService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TelegramPollingService.class);

    private final TelegramProperties telegramProperties;
    private final TelegramUpdateService telegramUpdateService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final AtomicLong offset = new AtomicLong(0);
    private final AtomicBoolean webhookCleared = new AtomicBoolean(false);

    public TelegramPollingService(TelegramProperties telegramProperties,
                                  TelegramUpdateService telegramUpdateService,
                                  ObjectMapper objectMapper) {
        this.telegramProperties = telegramProperties;
        this.telegramUpdateService = telegramUpdateService;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!usePolling()) {
            return;
        }
        clearWebhook();
    }

    @Scheduled(fixedDelay = 2000)
    public void poll() {
        if (!usePolling()) {
            return;
        }
        if (!webhookCleared.get()) {
            clearWebhook();
        }
        String url = "https://api.telegram.org/bot" + telegramProperties.botToken()
            + "/getUpdates?timeout=0&offset=" + offset.get();
        try {
            String body = restClient.get().uri(url).retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                return;
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode results = root.get("result");
            if (results == null || !results.isArray()) {
                return;
            }
            for (JsonNode update : results) {
                long updateId = update.path("update_id").asLong(0);
                if (updateId > 0) {
                    offset.set(updateId + 1);
                }
                telegramUpdateService.handle(update);
            }
        } catch (Exception ex) {
            log.debug("Telegram polling failed", ex);
        }
    }

    private boolean usePolling() {
        if (!telegramProperties.isConfigured()) {
            return false;
        }
        String publicUrl = telegramProperties.webhookPublicUrl();
        return publicUrl == null || publicUrl.isBlank();
    }

    private void clearWebhook() {
        try {
            String url = "https://api.telegram.org/bot" + telegramProperties.botToken() + "/deleteWebhook";
            restClient.post().uri(url).retrieve().toBodilessEntity();
            webhookCleared.set(true);
        } catch (Exception ex) {
            log.warn("Failed to clear Telegram webhook for polling mode", ex);
        }
    }
}
