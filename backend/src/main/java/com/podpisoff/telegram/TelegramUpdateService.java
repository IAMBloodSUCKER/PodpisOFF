package com.podpisoff.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.podpisoff.notification.TelegramNotificationService;
import com.podpisoff.settings.TelegramProperties;
import com.podpisoff.user.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TelegramUpdateService {

    private static final String CALLBACK_DISABLE = "disable_notifications";

    private final TelegramLinkService telegramLinkService;
    private final TelegramNotificationService telegramNotificationService;
    private final TelegramProperties telegramProperties;
    private final UserRepository userRepository;

    public TelegramUpdateService(TelegramLinkService telegramLinkService,
                                 TelegramNotificationService telegramNotificationService,
                                 TelegramProperties telegramProperties,
                                 UserRepository userRepository) {
        this.telegramLinkService = telegramLinkService;
        this.telegramNotificationService = telegramNotificationService;
        this.telegramProperties = telegramProperties;
        this.userRepository = userRepository;
    }

    public void handle(JsonNode update) {
        if (!telegramProperties.isConfigured()) {
            return;
        }

        JsonNode callback = update.get("callback_query");
        if (callback != null && !callback.isNull()) {
            handleCallback(callback);
            return;
        }

        JsonNode message = update.get("message");
        if (message == null || message.isNull()) {
            return;
        }
        JsonNode chat = message.get("chat");
        if (chat == null || chat.isNull()) {
            return;
        }
        String chatId = chat.path("id").asText(null);
        if (chatId == null || chatId.isBlank()) {
            return;
        }

        String text = message.path("text").asText("").trim();
        if (text.isBlank()) {
            return;
        }

        if (text.equals("/stop") || text.equalsIgnoreCase("отключить уведомления")) {
            disableNotifications(chatId);
            return;
        }

        if (text.startsWith("/start")) {
            String token = extractStartToken(text);
            if (token != null) {
                telegramLinkService.completeLink(token, chatId);
            } else {
                sendWelcome(chatId);
            }
            return;
        }

        if (text.equals("/help") || text.equals("/status")) {
            sendHelp(chatId);
        }
    }

    private void handleCallback(JsonNode callback) {
        String callbackId = callback.path("id").asText(null);
        String data = callback.path("data").asText("");
        JsonNode message = callback.get("message");
        if (message == null || message.isNull()) {
            return;
        }
        String chatId = message.path("chat").path("id").asText(null);
        if (chatId == null || chatId.isBlank()) {
            return;
        }

        if (CALLBACK_DISABLE.equals(data)) {
            disableNotifications(chatId);
            telegramNotificationService.answerCallback(callbackId, "Уведомления отключены");
        }
    }

    private void disableNotifications(String chatId) {
        if (!isLinked(chatId)) {
            telegramNotificationService.send(
                chatId,
                "Аккаунт не подключён. Подключите Telegram в настройках на сайте."
            );
            return;
        }
        telegramLinkService.handleStop(chatId);
    }

    private boolean isLinked(String chatId) {
        return userRepository.findByTelegramChatId(chatId.trim())
            .map(user -> user.getTelegramChatId() != null && user.isTelegramNotificationsEnabled())
            .orElse(false);
    }

    private static String extractStartToken(String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2) {
            return null;
        }
        String token = parts[1].trim();
        return token.isBlank() ? null : token;
    }

    private void sendWelcome(String chatId) {
        String username = telegramProperties.botUsername();
        telegramNotificationService.send(
            chatId,
            "Привет! Я бот ПодписOFF.\n\n"
                + "Чтобы подключить уведомления, откройте Настройки на сайте, примите условия и нажмите «Подключить Telegram».\n"
                + "Мы пришлём ссылку с QR-кодом — откройте её и нажмите Start.\n\n"
                + (username != null && !username.isBlank()
                ? "Ссылка из настроек: t.me/" + username + "\n\n"
                : "")
                + "/help — справка"
        );
    }

    private void sendHelp(String chatId) {
        if (isLinked(chatId)) {
            sendLinkedMessage(chatId);
            return;
        }
        telegramNotificationService.send(
            chatId,
            "ПодписOFF присылает:\n"
                + "• напоминания о списаниях по подпискам;\n"
                + "• ответы поддержки и сообщения администратора;\n"
                + "• уведомления о тарифе.\n\n"
                + "Подключение только через настройки аккаунта на сайте."
        );
    }

    private void sendLinkedMessage(String chatId) {
        telegramNotificationService.sendWithActions(
            chatId,
            "Уведомления включены.\n\n"
                + "Чтобы отключить — нажмите кнопку ниже, команду /stop или «Отключить» в настройках на сайте.",
            List.of(List.of(Map.of(
                "text", "Отключить уведомления",
                "callback_data", CALLBACK_DISABLE
            )))
        );
    }
}
