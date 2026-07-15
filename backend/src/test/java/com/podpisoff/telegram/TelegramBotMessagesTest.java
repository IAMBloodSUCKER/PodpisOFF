package com.podpisoff.telegram;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.podpisoff.user.LocaleCode;
import com.podpisoff.user.Plan;
import com.podpisoff.user.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TelegramBotMessagesTest {

    @Test
    void linkSuccessIncludesAccountDetails() {
        User user = sampleUser();
        String message = TelegramBotMessages.linkSuccess(user);

        assertTrue(message.contains("testuser"));
        assertTrue(message.contains("user@example.com"));
        assertTrue(message.contains("PRO"));
        assertTrue(message.contains("15.08.2026"));
        assertTrue(message.contains("напоминания о списаниях"));
    }

    @Test
    void linkedAccountStatusUsesLocale() {
        User user = sampleUser();
        user.setLocale(LocaleCode.EN);

        String message = TelegramBotMessages.linkedAccountStatus(user);

        assertTrue(message.contains("testuser"));
        assertTrue(message.contains("PRO"));
    }

    private static User sampleUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("user@example.com");
        user.setPlan(Plan.PRO);
        user.setPlanExpiresAt(LocalDateTime.of(2026, 8, 15, 12, 0));
        user.setLocale(LocaleCode.RU);
        return user;
    }
}
