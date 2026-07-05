package com.podpisoff.reminder;

import java.time.Instant;
import java.time.LocalDateTime;

public record ReminderResponse(
    Long id,
    String title,
    String note,
    LocalDateTime remindAt,
    boolean done,
    Instant createdAt,
    Instant updatedAt
) {
}
