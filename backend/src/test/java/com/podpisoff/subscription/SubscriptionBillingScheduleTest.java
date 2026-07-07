package com.podpisoff.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SubscriptionBillingScheduleTest {

    @Test
    void keepsFutureMonthlyDate() {
        LocalDate next = LocalDate.of(2026, 8, 9);
        assertEquals(next, SubscriptionBillingSchedule.effectiveNextDate(BillingPeriod.MONTHLY, next, LocalDate.of(2026, 7, 6)));
    }

    @Test
    void advancesMonthlyAfterChargeDay() {
        LocalDate next = LocalDate.of(2026, 7, 9);
        assertEquals(
            LocalDate.of(2026, 8, 9),
            SubscriptionBillingSchedule.effectiveNextDate(BillingPeriod.MONTHLY, next, LocalDate.of(2026, 7, 10))
        );
    }

    @Test
    void advancesYearlyAfterChargeDay() {
        LocalDate next = LocalDate.of(2025, 7, 9);
        assertEquals(
            LocalDate.of(2027, 7, 9),
            SubscriptionBillingSchedule.effectiveNextDate(BillingPeriod.YEARLY, next, LocalDate.of(2026, 7, 10))
        );
    }
}
