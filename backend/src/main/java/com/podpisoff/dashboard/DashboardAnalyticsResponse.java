package com.podpisoff.dashboard;

import com.podpisoff.subscription.BillingPeriod;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardAnalyticsResponse(
    List<MonthSpendPoint> monthlyTrend,
    List<TopSubscriptionItem> topByMonthlyCost,
    Map<String, Map<String, BigDecimal>> byCategory
) {
    public record MonthSpendPoint(int year, int month, Map<String, BigDecimal> spendByCurrency) {
    }

    public record TopSubscriptionItem(
        Long id,
        String title,
        String category,
        BigDecimal monthlyBurn,
        String currency,
        BillingPeriod billingPeriod
    ) {
    }
}
