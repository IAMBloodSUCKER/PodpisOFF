package com.podpisoff.subscription;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BillingPeriodMath {

    private BillingPeriodMath() {
    }

    public static BigDecimal monthlyBurn(BigDecimal amount, BillingPeriod period) {
        if (period == BillingPeriod.YEARLY) {
            return amount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal yearlyBurn(BigDecimal amount, BillingPeriod period) {
        if (period == BillingPeriod.MONTHLY) {
            return amount.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
