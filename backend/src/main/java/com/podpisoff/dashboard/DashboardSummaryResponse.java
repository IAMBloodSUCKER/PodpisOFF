package com.podpisoff.dashboard;

import com.podpisoff.subscription.SubscriptionResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardSummaryResponse(
    BigDecimal monthlyTotal,
    BigDecimal yearlyTotal,
    Map<String, BigDecimal> byCategory,
    List<SubscriptionResponse> upcomingBilling
) {
}
