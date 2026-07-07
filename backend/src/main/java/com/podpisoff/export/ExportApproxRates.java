package com.podpisoff.export;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/** Rough RUB rates for orientation in exports — same ballpark as the dashboard. */
final class ExportApproxRates {

    private static final Map<String, BigDecimal> RUB_PER_UNIT = Map.ofEntries(
        Map.entry("RUB", BigDecimal.ONE),
        Map.entry("USD", new BigDecimal("92")),
        Map.entry("EUR", new BigDecimal("99")),
        Map.entry("GBP", new BigDecimal("116")),
        Map.entry("CNY", new BigDecimal("12.7")),
        Map.entry("TRY", new BigDecimal("2.8")),
        Map.entry("KZT", new BigDecimal("0.19")),
        Map.entry("BYN", new BigDecimal("28")),
        Map.entry("UAH", new BigDecimal("2.3")),
        Map.entry("JPY", new BigDecimal("0.6")),
        Map.entry("CHF", new BigDecimal("105")),
        Map.entry("PLN", new BigDecimal("23")),
        Map.entry("AED", new BigDecimal("25")),
        Map.entry("THB", new BigDecimal("2.6")),
        Map.entry("INR", new BigDecimal("1.1"))
    );

    private ExportApproxRates() {
    }

    static BigDecimal approxMonthlyRub(Map<String, BigDecimal> monthlyByCurrency) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : monthlyByCurrency.entrySet()) {
            BigDecimal rate = RUB_PER_UNIT.get(entry.getKey().trim().toUpperCase());
            if (rate == null || entry.getValue().signum() <= 0) {
                continue;
            }
            total = total.add(entry.getValue().multiply(rate));
        }
        return total.setScale(0, RoundingMode.HALF_UP);
    }

    static boolean needsRubEquivalent(Map<String, BigDecimal> monthlyByCurrency) {
        long positive = monthlyByCurrency.entrySet().stream()
            .filter(entry -> entry.getValue().signum() > 0)
            .count();
        if (positive == 0) {
            return false;
        }
        if (positive == 1) {
            return !monthlyByCurrency.entrySet().stream()
                .filter(entry -> entry.getValue().signum() > 0)
                .findFirst()
                .map(entry -> "RUB".equalsIgnoreCase(entry.getKey()))
                .orElse(false);
        }
        return true;
    }
}
