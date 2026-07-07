package com.podpisoff.reminder;

import java.time.LocalDateTime;

public final class ReminderSchedule {

    private ReminderSchedule() {
    }

    public static LocalDateTime nextOccurrence(ReminderRepeat repeat, LocalDateTime remindAt, LocalDateTime from) {
        if (repeat == ReminderRepeat.ONCE) {
            return remindAt;
        }
        LocalDateTime cursor = remindAt;
        int guard = 0;
        while (!cursor.isAfter(from) && guard < 5000) {
            cursor = advance(repeat, cursor);
            guard++;
        }
        return cursor;
    }

    public static LocalDateTime advanceAfterAck(ReminderRepeat repeat, LocalDateTime remindAt, LocalDateTime now) {
        if (repeat == ReminderRepeat.ONCE) {
            return remindAt;
        }
        return nextOccurrence(repeat, remindAt, now);
    }

    private static LocalDateTime advance(ReminderRepeat repeat, LocalDateTime dateTime) {
        return switch (repeat) {
            case ONCE -> dateTime;
            case MONTHLY -> dateTime.plusMonths(1);
            case YEARLY -> dateTime.plusYears(1);
        };
    }
}
