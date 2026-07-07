package com.podpisoff.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.podpisoff.subscription.BillingPeriod;
import com.podpisoff.subscription.Subscription;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SubscriptionSpendingCalculatorTest {

    @Test
    void actualMonthSpendSkipsFutureBillingDates() {
        Subscription subscription = subscription(
            1L,
            new BigDecimal("1000.00"),
            "RUB",
            LocalDate.of(2026, 7, 20),
            BillingPeriod.MONTHLY,
            LocalDate.of(2026, 7, 1)
        );

        Map<String, BigDecimal> july = SubscriptionSpendingCalculator.actualMonthSpendByCurrency(
            List.of(subscription),
            Map.of(),
            2026,
            7,
            LocalDate.of(2026, 7, 7)
        );

        assertTrue(july.isEmpty());
    }

    @Test
    void actualMonthSpendCountsPastBillingDates() {
        Subscription subscription = subscription(
            1L,
            new BigDecimal("1000.00"),
            "RUB",
            LocalDate.of(2026, 8, 15),
            BillingPeriod.MONTHLY,
            LocalDate.of(2026, 2, 1)
        );

        Map<String, BigDecimal> june = SubscriptionSpendingCalculator.actualMonthSpendByCurrency(
            List.of(subscription),
            Map.of(),
            2026,
            6,
            LocalDate.of(2026, 7, 7)
        );

        assertEquals(new BigDecimal("1000.00"), june.get("RUB"));
    }

    @Test
    void actualMonthSpendSkipsMonthsBeforeSubscriptionCreated() {
        Subscription subscription = subscription(
            1L,
            new BigDecimal("500.00"),
            "RUB",
            LocalDate.of(2026, 8, 15),
            BillingPeriod.MONTHLY,
            LocalDate.of(2026, 7, 1)
        );

        Map<String, BigDecimal> june = SubscriptionSpendingCalculator.actualMonthSpendByCurrency(
            List.of(subscription),
            Map.of(),
            2026,
            6,
            LocalDate.of(2026, 7, 7)
        );

        assertTrue(june.isEmpty());
    }

    private static Subscription subscription(
        Long id,
        BigDecimal amount,
        String currency,
        LocalDate nextBillingDate,
        BillingPeriod billingPeriod,
        LocalDate createdAt
    ) {
        Subscription subscription = mock(Subscription.class);
        when(subscription.getId()).thenReturn(id);
        when(subscription.getAmount()).thenReturn(amount);
        when(subscription.getCurrency()).thenReturn(currency);
        when(subscription.getNextBillingDate()).thenReturn(nextBillingDate);
        when(subscription.getBillingPeriod()).thenReturn(billingPeriod);
        when(subscription.getCreatedAt()).thenReturn(
            createdAt.atStartOfDay(ZoneId.systemDefault()).toInstant()
        );
        when(subscription.isActive()).thenReturn(true);
        return subscription;
    }
}
