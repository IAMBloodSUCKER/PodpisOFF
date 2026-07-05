package com.podpisoff.billing;

import com.podpisoff.user.Plan;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
    @NotNull Plan targetPlan
) {
}
