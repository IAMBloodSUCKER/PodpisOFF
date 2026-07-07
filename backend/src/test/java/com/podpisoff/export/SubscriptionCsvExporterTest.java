package com.podpisoff.export;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.podpisoff.subscription.BillingPeriod;
import com.podpisoff.subscription.Subscription;
import com.podpisoff.user.LocaleCode;
import com.podpisoff.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SubscriptionCsvExporterTest {

    @Test
    void russianExportUsesSemicolonBomAndLocalizedHeaders() {
        Subscription subscription = sampleSubscription();
        String csv = SubscriptionCsvExporter.export(java.util.List.of(subscription), LocaleCode.RU);

        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("Название;Категория;Сумма"));
        assertTrue(csv.contains("1;cursor;Софт и VPN;60;USD;09.07.2026;Каждый месяц;Да"));
    }

    @Test
    void englishExportUsesCommaDelimiter() {
        String csv = SubscriptionCsvExporter.export(java.util.List.of(sampleSubscription()), LocaleCode.EN);
        assertTrue(csv.contains("Name,Category,Amount"));
        assertTrue(csv.contains("1,cursor,Софт и VPN,60,USD"));
    }

    private Subscription sampleSubscription() {
        Subscription subscription = new Subscription();
        subscription.setTitle("cursor");
        subscription.setCategory("Софт и VPN");
        subscription.setAmount(new BigDecimal("60.00"));
        subscription.setCurrency("USD");
        subscription.setNextBillingDate(LocalDate.of(2026, 7, 9));
        subscription.setBillingPeriod(BillingPeriod.MONTHLY);
        subscription.setActive(true);
        User user = new User();
        user.setUsername("test");
        subscription.setUser(user);
        return subscription;
    }
}
