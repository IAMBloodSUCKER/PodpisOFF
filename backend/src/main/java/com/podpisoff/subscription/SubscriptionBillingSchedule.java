package com.podpisoff.subscription;

import java.time.LocalDate;

public final class SubscriptionBillingSchedule {

    private SubscriptionBillingSchedule() {
    }

    public static LocalDate effectiveNextDate(BillingPeriod period, LocalDate nextBillingDate, LocalDate today) {
        LocalDate cursor = nextBillingDate;
        int guard = 0;
        while (!cursor.isAfter(today) && guard < 5000) {
            cursor = advance(period, cursor);
            guard++;
        }
        return cursor;
    }

    private static LocalDate advance(BillingPeriod period, LocalDate date) {
        return period == BillingPeriod.YEARLY ? date.plusYears(1) : date.plusMonths(1);
    }
}
