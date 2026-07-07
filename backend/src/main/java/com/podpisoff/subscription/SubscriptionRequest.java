package com.podpisoff.subscription;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionRequest(
    @NotBlank @Size(max = 120) String title,
    @NotBlank @Size(max = 80) String category,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank @Size(max = 8) String currency,
    @NotNull LocalDate nextBillingDate,
    BillingPeriod billingPeriod,
    boolean active,
    @Size(max = 500) String note,
    @Size(max = 500) String resourceUrl
) {
}
