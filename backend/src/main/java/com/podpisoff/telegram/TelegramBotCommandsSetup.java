package com.podpisoff.telegram;

import com.podpisoff.settings.TelegramProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TelegramBotCommandsSetup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotCommandsSetup.class);

    private final TelegramProperties telegramProperties;
    private final TelegramBotApiClient telegramBotApiClient;

    public TelegramBotCommandsSetup(TelegramProperties telegramProperties,
                                    TelegramBotApiClient telegramBotApiClient) {
        this.telegramProperties = telegramProperties;
        this.telegramBotApiClient = telegramBotApiClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!telegramProperties.isConfigured()) {
            return;
        }
        try {
            telegramBotApiClient.setMyCommands(List.of(
                command("start", "Открыть меню")
            ));
        } catch (Exception ex) {
            log.debug(
                "Failed to register Telegram bot commands: {}",
                TelegramSecrets.safeErrorMessage(ex, telegramBotApiClient.tokenForLogging())
            );
        }
    }

    private static Map<String, String> command(String name, String description) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("command", name);
        item.put("description", description);
        return item;
    }
}
