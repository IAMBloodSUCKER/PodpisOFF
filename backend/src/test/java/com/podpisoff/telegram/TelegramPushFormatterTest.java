package com.podpisoff.telegram;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TelegramPushFormatterTest {

    @Test
    void formatsTitleAndBodyWithHtml() {
        String message = TelegramPushFormatter.format(
            "📉 Тариф изменён на Free",
            "Тариф PRO отключён:\n• до 3 подписок\n• только рубли"
        );

        assertTrue(message.contains("<b>"));
        assertTrue(message.contains("Тариф изменён на Free"));
        assertTrue(message.contains("до 3 подписок"));
    }
}
