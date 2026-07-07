package com.podpisoff.settings;

import java.util.Set;

public final class BillingReminderDays {

    public static final int DEFAULT = 3;
    public static final Set<Integer> ALLOWED = Set.of(0, 1, 3, 7, 14);

    private BillingReminderDays() {
    }

    public static int normalize(Integer value) {
        if (value == null) {
            return DEFAULT;
        }
        if (!ALLOWED.contains(value)) {
            throw new IllegalArgumentException("Unsupported billing reminder offset");
        }
        return value;
    }
}
