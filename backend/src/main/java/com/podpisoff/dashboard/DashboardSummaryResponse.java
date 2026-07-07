package com.podpisoff.dashboard;

import com.podpisoff.subscription.SubscriptionResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardSummaryResponse(
    int selectedYear,
    int selectedMonth,
    Map<String, BigDecimal> monthlyByCurrency,
    Map<String, BigDecimal> yearlyByCurrency,
    Map<String, BigDecimal> monthSpendByCurrency,
    Map<String, Map<String, BigDecimal>> byCategory,
    List<SubscriptionResponse> upcomingBilling
) {
}
