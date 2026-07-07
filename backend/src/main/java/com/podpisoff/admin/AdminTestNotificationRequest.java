package com.podpisoff.admin;

import jakarta.validation.constraints.NotNull;

public record AdminTestNotificationRequest(@NotNull Integer delaySeconds) {
}
