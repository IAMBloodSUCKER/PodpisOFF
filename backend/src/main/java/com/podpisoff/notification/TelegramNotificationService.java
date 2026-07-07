package com.podpisoff.notification;

import com.podpisoff.settings.TelegramProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final TelegramProperties telegramProperties;
    private final RestClient restClient;

    public TelegramNotificationService(TelegramProperties telegramProperties) {
        this.telegramProperties = telegramProperties;
        this.restClient = RestClient.create();
    }

    public boolean isConfigured() {
        return telegramProperties.isConfigured();
    }

    public void send(String chatId, String text) {
        send(chatId, text, null);
    }

    public void sendWithActions(String chatId, String text, List<List<Map<String, String>>> inlineKeyboard) {
        Map<String, Object> replyMarkup = Map.of("inline_keyboard", inlineKeyboard);
        send(chatId, text, replyMarkup);
    }

    public void answerCallback(String callbackQueryId, String text) {
        if (!isConfigured() || callbackQueryId == null || callbackQueryId.isBlank()) {
            return;
        }
        String url = "https://api.telegram.org/bot" + telegramProperties.botToken() + "/answerCallbackQuery";
        try {
            LinkedHashMap<String, Object> body = new LinkedHashMap<>();
            body.put("callback_query_id", callbackQueryId);
            if (text != null && !text.isBlank()) {
                body.put("text", text);
            }
            restClient.post().uri(url).body(body).retrieve().toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to answer Telegram callback {}", callbackQueryId, ex);
        }
    }

    private void send(String chatId, String text, Map<String, Object> replyMarkup) {
        if (!isConfigured() || chatId == null || chatId.isBlank() || text == null || text.isBlank()) {
            return;
        }
        String url = "https://api.telegram.org/bot" + telegramProperties.botToken() + "/sendMessage";
        try {
            LinkedHashMap<String, Object> body = new LinkedHashMap<>();
            body.put("chat_id", chatId.trim());
            body.put("text", text);
            if (replyMarkup != null) {
                body.put("reply_markup", replyMarkup);
            }
            restClient.post().uri(url).body(body).retrieve().toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to send Telegram notification to {}", chatId, ex);
        }
    }
}
