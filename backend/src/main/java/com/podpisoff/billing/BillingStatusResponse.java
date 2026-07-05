package com.podpisoff.billing;

import com.podpisoff.user.Plan;
import java.time.LocalDateTime;

public record BillingStatusResponse(
    Plan plan,
    LocalDateTime planExpiresAt
) {
}
