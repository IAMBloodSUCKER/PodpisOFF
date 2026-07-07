package com.podpisoff.reminder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ReminderRequest(
    @NotBlank @Size(max = 150) String title,
    @Size(max = 1000) String note,
    @NotNull LocalDateTime remindAt,
    @NotNull ReminderRepeat repeat,
    boolean done
) {
}
