package com.podpisoff.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SubscriptionBillingCalendarTest {

    @Test
    void monthlySubscriptionAppliesEveryMonth() {
        assertTrue(SubscriptionBillingCalendar.appliesToCalendarMonth(
            BillingPeriod.MONTHLY,
            LocalDate.of(2026, 7, 15),
            2026,
            3
        ));
    }

    @Test
    void yearlySubscriptionOnlyInAnniversaryMonth() {
        LocalDate next = LocalDate.of(2026, 7, 15);
        assertTrue(SubscriptionBillingCalendar.appliesToCalendarMonth(BillingPeriod.YEARLY, next, 2026, 7));
        assertFalse(SubscriptionBillingCalendar.appliesToCalendarMonth(BillingPeriod.YEARLY, next, 2026, 3));
    }

    @Test
    void plannedChargeUsesAmountWhenActive() {
        Subscription subscription = new Subscription();
        subscription.setActive(true);
        subscription.setAmount(new BigDecimal("60.00"));
        subscription.setBillingPeriod(BillingPeriod.MONTHLY);
        subscription.setNextBillingDate(LocalDate.of(2026, 7, 1));

        assertEquals(new BigDecimal("60.00"), SubscriptionBillingCalendar.plannedCharge(subscription, 2026, 3));
    }
}
