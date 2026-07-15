package com.podpisoff.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.podpisoff.dashboard.DashboardAnalyticsResponse;
import com.podpisoff.dashboard.DashboardSummaryResponse;
import com.podpisoff.subscription.BillingPeriod;
import com.podpisoff.subscription.SubscriptionResponse;
import com.podpisoff.user.LocaleCode;
import com.podpisoff.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TelegramBotReplyFormatterTest {

    @Test
    void menuActionParsesCallback() {
        assertEquals(TelegramMenuAction.SUBS, TelegramMenuAction.fromCallback("m:s"));
        assertEquals(TelegramMenuAction.HOME, TelegramMenuAction.fromCallback("m:h"));
        assertEquals(TelegramMenuAction.ANALYTICS, TelegramMenuAction.fromCallback("m:a"));
        assertEquals(TelegramMenuAction.EXPORT, TelegramMenuAction.fromCallback("m:x"));
    }

    @Test
    void homeIncludesAccountBlock() {
        User user = sampleUser();
        String message = TelegramBotReplyFormatter.home(user, "https://podpisoff.app");

        assertTrue(message.contains("ПодписOFF"));
        assertTrue(message.contains("Telegram подключён"));
        assertTrue(message.contains("кнопками"));
    }

    @Test
    void subscriptionsListIncludesTitleAndAmount() {
        User user = sampleUser();
        List<SubscriptionResponse> subscriptions = List.of(
            new SubscriptionResponse(
                1L,
                "Netflix",
                "video",
                new BigDecimal("799.00"),
                "RUB",
                LocalDate.of(2026, 7, 15),
                BillingPeriod.MONTHLY,
                true,
                null,
                null,
                Instant.now(),
                Instant.now()
            )
        );

        String message = TelegramBotReplyFormatter.subscriptions(user, subscriptions);

        assertTrue(message.contains("Netflix"));
        assertTrue(message.contains("799 RUB"));
        assertTrue(message.contains("активна"));
    }

    @Test
    void summaryIncludesMonthlyTotals() {
        User user = sampleUser();
        DashboardSummaryResponse summary = new DashboardSummaryResponse(
            2026,
            7,
            Map.of("RUB", new BigDecimal("1500")),
            Map.of("RUB", new BigDecimal("18000")),
            Map.of("RUB", new BigDecimal("1200")),
            Map.of("video", Map.of("RUB", new BigDecimal("799"))),
            List.of()
        );

        String message = TelegramBotReplyFormatter.summary(user, summary);

        assertTrue(message.contains("Сводка"));
        assertTrue(message.contains("1500 RUB"));
        assertTrue(message.contains("video"));
    }

    @Test
    void analyticsIncludesTrendAndTopSubscriptions() {
        User user = sampleUser();
        DashboardAnalyticsResponse analytics = new DashboardAnalyticsResponse(
            List.of(new DashboardAnalyticsResponse.MonthSpendPoint(2026, 6, Map.of("RUB", new BigDecimal("1200")))),
            List.of(new DashboardAnalyticsResponse.TopSubscriptionItem(
                1L,
                "Netflix",
                "video",
                new BigDecimal("799"),
                "RUB",
                BillingPeriod.MONTHLY
            )),
            Map.of("video", Map.of("RUB", new BigDecimal("799")))
        );

        String message = TelegramBotReplyFormatter.analytics(user, analytics);

        assertTrue(message.contains("Аналитика"));
        assertTrue(message.contains("Netflix"));
        assertTrue(message.contains("1200 RUB"));
    }

    @Test
    void freeSubscriptionsNoteShownWhenOverLimit() {
        User user = sampleUser();
        List<SubscriptionResponse> subscriptions = List.of(
            subscription("One"),
            subscription("Two"),
            subscription("Three"),
            subscription("Four")
        );

        String message = TelegramBotReplyFormatter.subscriptions(user, subscriptions, false, 3);

        assertTrue(message.contains("3 старейшие"));
    }

    private static SubscriptionResponse subscription(String title) {
        return new SubscriptionResponse(
            1L,
            title,
            "misc",
            new BigDecimal("100"),
            "RUB",
            LocalDate.of(2026, 7, 15),
            BillingPeriod.MONTHLY,
            true,
            null,
            null,
            Instant.now(),
            Instant.now()
        );
    }

    private static User sampleUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setLocale(LocaleCode.RU);
        return user;
    }
}
