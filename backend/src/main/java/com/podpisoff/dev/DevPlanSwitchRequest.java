package com.podpisoff.dev;

import com.podpisoff.user.Plan;
import jakarta.validation.constraints.NotNull;

public record DevPlanSwitchRequest(@NotNull Plan plan, Boolean expired) {
}
