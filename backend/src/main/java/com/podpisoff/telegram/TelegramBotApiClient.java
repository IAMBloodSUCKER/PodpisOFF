package com.podpisoff.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.podpisoff.settings.TelegramProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class TelegramBotApiClient {

    private final TelegramProperties telegramProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TelegramBotApiClient(TelegramProperties telegramProperties, ObjectMapper objectMapper) {
        this.telegramProperties = telegramProperties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public boolean isConfigured() {
        return telegramProperties.isConfigured();
    }

    public JsonNode getUpdates(long offset) throws Exception {
        String body = restClient.get()
            .uri(apiUrl("getUpdates") + "?timeout=0&offset=" + offset)
            .retrieve()
            .body(String.class);
        if (body == null || body.isBlank()) {
            return null;
        }
        return objectMapper.readTree(body);
    }

    public void deleteWebhook() {
        restClient.post().uri(apiUrl("deleteWebhook")).retrieve().toBodilessEntity();
    }

    public void setWebhook(String webhookUrl, String secretToken) {
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("url", webhookUrl);
        if (secretToken != null && !secretToken.isBlank()) {
            body.put("secret_token", secretToken);
        }
        restClient.post().uri(apiUrl("setWebhook")).body(body).retrieve().toBodilessEntity();
    }

    public void sendMessage(String chatId, String text, Map<String, Object> replyMarkup) {
        sendMessageReturnId(chatId, text, replyMarkup);
    }

    public void sendDocument(String chatId, byte[] data, String filename) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Document data is empty");
        }
        String safeName = filename == null || filename.isBlank() ? "export.xlsx" : filename.trim();
        ByteArrayResource resource = new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return safeName;
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId.trim());
        body.add("document", resource);
        restClient.post()
            .uri(apiUrl("sendDocument"))
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .toBodilessEntity();
    }

    public Long sendMessageReturnId(String chatId, String text, Map<String, Object> replyMarkup) {
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", chatId.trim());
        body.put("text", text);
        body.put("parse_mode", "HTML");
        if (replyMarkup != null) {
            body.put("reply_markup", replyMarkup);
        }
        try {
            String response = restClient.post().uri(apiUrl("sendMessage")).body(body).retrieve().body(String.class);
            if (response == null || response.isBlank()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(response);
            long messageId = root.path("result").path("message_id").asLong(0);
            return messageId > 0 ? messageId : null;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void editMessageText(String chatId, long messageId, String text, Map<String, Object> replyMarkup) {
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", chatId.trim());
        body.put("message_id", messageId);
        body.put("text", text);
        body.put("parse_mode", "HTML");
        if (replyMarkup != null) {
            body.put("reply_markup", replyMarkup);
        }
        restClient.post().uri(apiUrl("editMessageText")).body(body).retrieve().toBodilessEntity();
    }

    public void deleteMessage(String chatId, long messageId) {
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", chatId.trim());
        body.put("message_id", messageId);
        restClient.post().uri(apiUrl("deleteMessage")).body(body).retrieve().toBodilessEntity();
    }

    public void answerCallbackQuery(String callbackQueryId, String text) {
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("callback_query_id", callbackQueryId);
        if (text != null && !text.isBlank()) {
            body.put("text", text);
        }
        restClient.post().uri(apiUrl("answerCallbackQuery")).body(body).retrieve().toBodilessEntity();
    }

    public void setMyCommands(List<Map<String, String>> commands) {
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("commands", commands);
        restClient.post().uri(apiUrl("setMyCommands")).body(body).retrieve().toBodilessEntity();
    }

    public String tokenForLogging() {
        return telegramProperties.botToken();
    }

    private String apiUrl(String method) {
        return "https://api.telegram.org/bot" + telegramProperties.botToken() + "/" + method;
    }
}
