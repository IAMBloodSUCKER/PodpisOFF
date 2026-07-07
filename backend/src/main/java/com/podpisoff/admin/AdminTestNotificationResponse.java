package com.podpisoff.admin;

import java.time.Instant;

public record AdminTestNotificationResponse(int delaySeconds, Instant deliverAt) {
}
