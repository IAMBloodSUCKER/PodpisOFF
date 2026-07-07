package com.podpisoff.subscription;

import java.math.BigDecimal;

public record SubscriptionMonthChargeResponse(
    Long id,
    Long subscriptionId,
    int year,
    int month,
    BigDecimal amount,
    String note
) {
}
