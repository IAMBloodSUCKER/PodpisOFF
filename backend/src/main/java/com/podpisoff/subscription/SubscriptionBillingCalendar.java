package com.podpisoff.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public final class SubscriptionBillingCalendar {

    private SubscriptionBillingCalendar() {
    }

    public static boolean appliesToCalendarMonth(BillingPeriod period, LocalDate nextBillingDate, int year, int month) {
        if (period == BillingPeriod.MONTHLY) {
            return true;
        }
        return nextBillingDate.getMonthValue() == month;
    }

    public static BigDecimal plannedCharge(Subscription subscription, int year, int month) {
        if (!subscription.isActive()) {
            return BigDecimal.ZERO;
        }
        if (!appliesToCalendarMonth(subscription.getBillingPeriod(), subscription.getNextBillingDate(), year, month)) {
            return BigDecimal.ZERO;
        }
        return subscription.getAmount();
    }

    public static LocalDate clampToMonth(LocalDate date, YearMonth month) {
        int day = Math.min(date.getDayOfMonth(), month.lengthOfMonth());
        return LocalDate.of(month.getYear(), month.getMonthValue(), day);
    }
}
