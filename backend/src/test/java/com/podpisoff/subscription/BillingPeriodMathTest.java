package com.podpisoff.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BillingPeriodMathTest {

    @Test
    void monthlySubscriptionBurn() {
        assertEquals(new BigDecimal("60.00"), BillingPeriodMath.monthlyBurn(new BigDecimal("60"), BillingPeriod.MONTHLY));
        assertEquals(new BigDecimal("720.00"), BillingPeriodMath.yearlyBurn(new BigDecimal("60"), BillingPeriod.MONTHLY));
    }

    @Test
    void yearlySubscriptionBurn() {
        assertEquals(new BigDecimal("10.00"), BillingPeriodMath.monthlyBurn(new BigDecimal("120"), BillingPeriod.YEARLY));
        assertEquals(new BigDecimal("120.00"), BillingPeriodMath.yearlyBurn(new BigDecimal("120"), BillingPeriod.YEARLY));
    }
}
