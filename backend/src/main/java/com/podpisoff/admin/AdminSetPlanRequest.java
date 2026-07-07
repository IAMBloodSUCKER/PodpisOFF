package com.podpisoff.admin;

import com.podpisoff.user.Plan;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AdminSetPlanRequest(
    @NotNull Plan plan,
    LocalDateTime planExpiresAt
) {
}
