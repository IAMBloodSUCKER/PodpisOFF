package com.podpisoff.dashboard;

import com.podpisoff.subscription.BillingPeriod;
import com.podpisoff.subscription.BillingPeriodMath;
import com.podpisoff.subscription.Subscription;
import com.podpisoff.subscription.SubscriptionBillingCalendar;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SubscriptionSpendingCalculator {

    private SubscriptionSpendingCalculator() {
    }

    public static Map<String, BigDecimal> monthSpendByCurrency(
        List<Subscription> active,
        Map<Long, BigDecimal> overrides,
        int year,
        int month
    ) {
        Map<String, BigDecimal> monthSpendByCurrency = new LinkedHashMap<>();
        active.forEach(subscription -> {
            BigDecimal planned = SubscriptionBillingCalendar.plannedCharge(subscription, year, month);
            if (planned.compareTo(BigDecimal.ZERO) == 0) {
                return;
            }
            BigDecimal actual = overrides.getOrDefault(subscription.getId(), planned);
            monthSpendByCurrency.merge(subscription.getCurrency(), actual, BigDecimal::add);
        });
        return monthSpendByCurrency;
    }

    /** Past billing dates only — for spend history charts. */
    public static Map<String, BigDecimal> actualMonthSpendByCurrency(
        List<Subscription> subscriptions,
        Map<Long, BigDecimal> overrides,
        int year,
        int month,
        LocalDate today
    ) {
        Map<String, BigDecimal> monthSpendByCurrency = new LinkedHashMap<>();
        YearMonth target = YearMonth.of(year, month);
        subscriptions.forEach(subscription -> {
            LocalDate billingDate = billingDateInMonth(subscription, target);
            if (billingDate == null) {
                return;
            }
            if (billingDate.isAfter(today)) {
                return;
            }
            LocalDate created = LocalDate.ofInstant(subscription.getCreatedAt(), ZoneId.systemDefault());
            if (billingDate.isBefore(created)) {
                return;
            }
            if (!isOnBillingSchedule(subscription, billingDate)) {
                return;
            }
            BigDecimal amount = overrides.getOrDefault(subscription.getId(), subscription.getAmount());
            monthSpendByCurrency.merge(subscription.getCurrency(), amount, BigDecimal::add);
        });
        return monthSpendByCurrency;
    }

    private static LocalDate billingDateInMonth(Subscription subscription, YearMonth month) {
        if (!SubscriptionBillingCalendar.appliesToCalendarMonth(
            subscription.getBillingPeriod(),
            subscription.getNextBillingDate(),
            month.getYear(),
            month.getMonthValue()
        )) {
            return null;
        }
        int day = Math.min(subscription.getNextBillingDate().getDayOfMonth(), month.lengthOfMonth());
        return LocalDate.of(month.getYear(), month.getMonthValue(), day);
    }

    private static boolean isOnBillingSchedule(Subscription subscription, LocalDate billingDate) {
        LocalDate cursor = subscription.getNextBillingDate();
        int guard = 0;
        while (guard < 5000) {
            if (cursor.equals(billingDate)) {
                return true;
            }
            if (cursor.isBefore(billingDate)) {
                return false;
            }
            cursor = subscription.getBillingPeriod() == BillingPeriod.YEARLY
                ? cursor.minusYears(1)
                : cursor.minusMonths(1);
            guard++;
        }
        return false;
    }

    public static BigDecimal monthlyBurn(Subscription subscription) {
        return BillingPeriodMath.monthlyBurn(subscription.getAmount(), subscription.getBillingPeriod());
    }

    public static Map<String, Map<String, BigDecimal>> byCategoryMonthlyBurn(List<Subscription> active) {
        Map<String, Map<String, BigDecimal>> byCategory = new LinkedHashMap<>();
        active.forEach(subscription -> byCategory
            .computeIfAbsent(subscription.getCategory(), key -> new LinkedHashMap<>())
            .merge(
                subscription.getCurrency(),
                monthlyBurn(subscription),
                BigDecimal::add
            ));
        return byCategory;
    }
}
