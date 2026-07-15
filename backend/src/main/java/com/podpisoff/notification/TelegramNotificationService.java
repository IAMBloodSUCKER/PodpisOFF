package com.podpisoff.notification;



import com.podpisoff.settings.TelegramProperties;

import com.podpisoff.telegram.TelegramBotApiClient;

import com.podpisoff.telegram.TelegramMenuRefreshScheduler;

import com.podpisoff.telegram.TelegramPushFormatter;

import com.podpisoff.telegram.TelegramSecrets;

import java.util.List;

import java.util.Map;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;



@Service

public class TelegramNotificationService {



    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);



    private final TelegramProperties telegramProperties;

    private final TelegramBotApiClient telegramBotApiClient;

    private final TelegramMenuRefreshScheduler menuRefreshScheduler;



    public TelegramNotificationService(TelegramProperties telegramProperties,

                                       TelegramBotApiClient telegramBotApiClient,

                                       TelegramMenuRefreshScheduler menuRefreshScheduler) {

        this.telegramProperties = telegramProperties;

        this.telegramBotApiClient = telegramBotApiClient;

        this.menuRefreshScheduler = menuRefreshScheduler;

    }



    public boolean isConfigured() {

        return telegramProperties.isConfigured();

    }



    public void sendPush(String chatId, String title, String body) {

        if (!isConfigured() || chatId == null || chatId.isBlank() || title == null || title.isBlank()) {

            return;

        }

        try {

            String message = TelegramPushFormatter.format(title, body);

            telegramBotApiClient.sendMessage(chatId, message, null);

            menuRefreshScheduler.scheduleRefresh(chatId);

        } catch (Exception ex) {

            log.warn(

                "Failed to send Telegram notification to {}: {}",

                chatId,

                safeError(ex)

            );

        }

    }



    public void sendWithActions(String chatId, String text, List<List<Map<String, String>>> inlineKeyboard) {

        Map<String, Object> replyMarkup = Map.of("inline_keyboard", inlineKeyboard);

        sendFormatted(chatId, text, replyMarkup);

    }



    public void answerCallback(String callbackQueryId, String text) {

        if (!isConfigured() || callbackQueryId == null || callbackQueryId.isBlank()) {

            return;

        }

        try {

            telegramBotApiClient.answerCallbackQuery(callbackQueryId, text);

        } catch (Exception ex) {

            log.warn(

                "Failed to answer Telegram callback {}: {}",

                callbackQueryId,

                safeError(ex)

            );

        }

    }



    private void sendFormatted(String chatId, String text, Map<String, Object> replyMarkup) {

        if (!isConfigured() || chatId == null || chatId.isBlank() || text == null || text.isBlank()) {

            return;

        }

        try {

            telegramBotApiClient.sendMessage(chatId, text, replyMarkup);

        } catch (Exception ex) {

            log.warn(

                "Failed to send Telegram notification to {}: {}",

                chatId,

                safeError(ex)

            );

        }

    }



    private String safeError(Throwable error) {

        return TelegramSecrets.safeErrorMessage(error, telegramBotApiClient.tokenForLogging());

    }

}

