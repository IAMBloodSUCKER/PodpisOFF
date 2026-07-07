package com.podpisoff.common;

import java.util.Set;

public final class SupportedCurrencies {

    private static final Set<String> CODES = Set.of(
        "RUB", "USD", "EUR", "GBP", "CNY", "TRY", "KZT", "BYN", "UAH", "JPY", "CHF", "PLN", "AED", "THB", "INR"
    );

    private SupportedCurrencies() {
    }

    public static boolean isSupported(String currency) {
        if (currency == null || currency.isBlank()) {
            return false;
        }
        return CODES.contains(currency.trim().toUpperCase());
    }

    public static String normalize(String currency) {
        return currency.trim().toUpperCase();
    }
}
