package com.podpisoff.export;

import com.podpisoff.subscription.BillingPeriod;
import com.podpisoff.subscription.Subscription;
import com.podpisoff.user.LocaleCode;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class SubscriptionCsvExporter {

    private static final DateTimeFormatter DATE_RU = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_EN = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private SubscriptionCsvExporter() {
    }

    public static String export(List<Subscription> subscriptions, LocaleCode locale) {
        boolean russian = locale == LocaleCode.RU;
        char delimiter = russian ? ';' : ',';
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append(String.join(String.valueOf(delimiter), headers(locale)));
        int index = 0;
        for (Subscription subscription : subscriptions) {
            index++;
            sb.append('\n').append(row(index, subscription, russian, delimiter));
        }
        return sb.toString();
    }

    private static String[] headers(LocaleCode locale) {
        if (locale == LocaleCode.RU) {
            return new String[] {
                "№",
                "Название",
                "Категория",
                "Сумма",
                "Валюта",
                "Следующее списание",
                "Период",
                "Активна",
                "Комментарий",
                "Ссылка"
            };
        }
        return new String[] {
            "ID",
            "Name",
            "Category",
            "Amount",
            "Currency",
            "Next billing",
            "Period",
            "Active",
            "Note",
            "Link"
        };
    }

    private static String row(int index, Subscription subscription, boolean russian, char delimiter) {
        return String.join(
            String.valueOf(delimiter),
            String.valueOf(index),
            escape(subscription.getTitle(), delimiter),
            escape(subscription.getCategory(), delimiter),
            escape(formatAmount(subscription.getAmount(), russian), delimiter),
            escape(subscription.getCurrency(), delimiter),
            escape(formatDate(subscription.getNextBillingDate(), russian), delimiter),
            escape(formatPeriod(subscription.getBillingPeriod(), russian), delimiter),
            escape(formatActive(subscription.isActive(), russian), delimiter),
            escape(subscription.getNote() == null ? "" : subscription.getNote(), delimiter),
            escape(subscription.getResourceUrl() == null ? "" : subscription.getResourceUrl(), delimiter)
        );
    }

    private static String formatAmount(BigDecimal amount, boolean russian) {
        String plain = amount.stripTrailingZeros().scale() <= 0
            ? amount.setScale(0).toPlainString()
            : amount.setScale(2).toPlainString();
        return russian ? plain.replace('.', ',') : plain;
    }

    private static String formatDate(java.time.LocalDate date, boolean russian) {
        return russian ? date.format(DATE_RU) : date.format(DATE_EN);
    }

    private static String formatPeriod(BillingPeriod period, boolean russian) {
        if (period == BillingPeriod.YEARLY) {
            return russian ? "Каждый год" : "Yearly";
        }
        return russian ? "Каждый месяц" : "Monthly";
    }

    private static String formatActive(boolean active, boolean russian) {
        if (russian) {
            return active ? "Да" : "Нет";
        }
        return active ? "Yes" : "No";
    }

    private static String escape(String value, char delimiter) {
        String escaped = value.replace("\"", "\"\"");
        boolean needsQuotes = value.contains(String.valueOf(delimiter))
            || value.contains("\"")
            || value.contains("\n")
            || value.contains("\r");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
