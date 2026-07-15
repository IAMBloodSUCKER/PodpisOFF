package com.podpisoff.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.podpisoff.settings.TelegramProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telegram")
public class TelegramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);

    private final TelegramUpdateService telegramUpdateService;
    private final TelegramProperties telegramProperties;
    private final ObjectMapper objectMapper;

    public TelegramWebhookController(TelegramUpdateService telegramUpdateService,
                                       TelegramProperties telegramProperties,
                                       ObjectMapper objectMapper) {
        this.telegramUpdateService = telegramUpdateService;
        this.telegramProperties = telegramProperties;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
        @RequestBody String body,
        @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken
    ) {
        if (!telegramProperties.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!isAuthorized(secretToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            JsonNode update = objectMapper.readTree(body);
            telegramUpdateService.handle(update);
        } catch (Exception ex) {
            log.warn("Failed to process Telegram update: {}", TelegramSecrets.safeErrorMessage(ex, null));
        }
        return ResponseEntity.ok().build();
    }

    private boolean isAuthorized(String secretToken) {
        String expected = telegramProperties.webhookSecret();
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return expected.equals(secretToken);
    }
}
