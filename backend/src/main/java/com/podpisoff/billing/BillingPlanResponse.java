package com.podpisoff.billing;

import com.podpisoff.user.Plan;
import java.math.BigDecimal;
import java.util.List;

public record BillingPlanResponse(
    Plan id,
    BigDecimal priceRub,
    BigDecimal priceUsd,
    BigDecimal priceYearRub,
    BigDecimal priceYearUsd,
    int subscriptionLimit,
    int reminderLimit,
    List<String> featureKeys
) {
}
