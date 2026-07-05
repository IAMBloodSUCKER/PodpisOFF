package com.podpisoff.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SubscriptionResponse(
    Long id,
    String title,
    String category,
    BigDecimal amount,
    String currency,
    LocalDate nextBillingDate,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
