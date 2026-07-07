package com.podpisoff.reminder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ReminderScheduleTest {

    @Test
    void yearlyAdvancesToNextYearWhenPast() {
        LocalDateTime anchor = LocalDateTime.of(2024, 3, 15, 10, 0);
        LocalDateTime from = LocalDateTime.of(2026, 7, 5, 12, 0);
        LocalDateTime next = ReminderSchedule.nextOccurrence(ReminderRepeat.YEARLY, anchor, from);
        assertEquals(LocalDateTime.of(2027, 3, 15, 10, 0), next);
    }

    @Test
    void monthlyAdvancesWithinSameYear() {
        LocalDateTime anchor = LocalDateTime.of(2026, 1, 15, 9, 30);
        LocalDateTime from = LocalDateTime.of(2026, 7, 5, 12, 0);
        LocalDateTime next = ReminderSchedule.nextOccurrence(ReminderRepeat.MONTHLY, anchor, from);
        assertEquals(LocalDateTime.of(2026, 7, 15, 9, 30), next);
    }
}
