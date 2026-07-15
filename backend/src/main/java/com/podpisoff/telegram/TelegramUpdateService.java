package com.podpisoff.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.podpisoff.notification.TelegramNotificationService;
import com.podpisoff.settings.TelegramProperties;
import com.podpisoff.user.User;
import com.podpisoff.user.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TelegramUpdateService {

    private final TelegramLinkService telegramLinkService;
    private final TelegramNotificationService telegramNotificationService;
    private final TelegramProperties telegramProperties;
    private final UserRepository userRepository;
    private final TelegramBotMenuService telegramBotMenuService;

    public TelegramUpdateService(TelegramLinkService telegramLinkService,
                                 TelegramNotificationService telegramNotificationService,
                                 TelegramProperties telegramProperties,
                                 UserRepository userRepository,
                                 TelegramBotMenuService telegramBotMenuService) {
        this.telegramLinkService = telegramLinkService;
        this.telegramNotificationService = telegramNotificationService;
        this.telegramProperties = telegramProperties;
        this.userRepository = userRepository;
        this.telegramBotMenuService = telegramBotMenuService;
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

        long incomingMessageId = message.path("message_id").asLong(0);

        if (text.startsWith("/start")) {
            handleStart(chatId, text);
            deleteIncomingMessage(chatId, incomingMessageId);
            return;
        }

        if (isStopCommand(text)) {
            disableFromChat(chatId);
            deleteIncomingMessage(chatId, incomingMessageId);
            return;
        }

        deleteIncomingMessage(chatId, incomingMessageId);
    }

    private void handleStart(String chatId, String text) {
        String token = extractStartToken(text);
        if (token != null) {
            Optional<User> linked = telegramLinkService.completeLink(token, chatId);
            if (linked.isPresent()) {
                telegramBotMenuService.openLinkedMenu(linked.get(), chatId, TelegramMenuAction.HOME);
                return;
            }
            telegramBotMenuService.showInvalidLink(chatId);
            return;
        }
        telegramBotMenuService.openMenuForChat(chatId);
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

        TelegramMenuAction action = TelegramMenuAction.fromCallback(data);
        if (action == null) {
            return;
        }

        long messageId = message.path("message_id").asLong(0);
        if (messageId <= 0) {
            return;
        }
        telegramBotMenuService.handleMenuAction(chatId, messageId, action, callbackId);
    }

    private void disableFromChat(String chatId) {
        Optional<User> linkedUser = userRepository.findByTelegramChatId(chatId.trim())
            .filter(user -> user.getTelegramChatId() != null && user.isTelegramNotificationsEnabled());
        if (linkedUser.isEmpty()) {
            telegramBotMenuService.showUnlinkedPublic(chatId);
            return;
        }
        Long messageId = linkedUser.get().getTelegramMenuMessageId();
        if (messageId != null && messageId > 0) {
            telegramBotMenuService.handleMenuAction(chatId, messageId, TelegramMenuAction.DISABLE, null);
            return;
        }
        telegramLinkService.handleStop(chatId);
        telegramBotMenuService.showUnlinkedPublic(chatId);
    }

    private void deleteIncomingMessage(String chatId, long messageId) {
        if (messageId > 0) {
            telegramBotMenuService.deleteChatMessage(chatId, messageId);
        }
    }

    private static boolean isStopCommand(String text) {
        String command = text.trim().split("\\s+", 2)[0].toLowerCase();
        int at = command.indexOf('@');
        if (at > 0) {
            command = command.substring(0, at);
        }
        return "/stop".equals(command);
    }

    private static String extractStartToken(String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2) {
            return null;
        }
        String token = parts[1].trim();
        return token.isBlank() ? null : token;
    }
}
