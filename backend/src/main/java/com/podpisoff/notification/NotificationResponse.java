package com.podpisoff.notification;

import java.time.Instant;

public record NotificationResponse(
    Long id,
    String type,
    String title,
    String body,
    Long referenceId,
    Instant createdAt,
    Instant readAt,
    boolean unread
) {
}
