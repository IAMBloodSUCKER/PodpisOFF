package com.podpisoff.export;

import com.podpisoff.subscription.BillingPeriod;
import com.podpisoff.user.LocaleCode;

public final class ExportLabels {

    private ExportLabels() {
    }

    public static String sheetData(LocaleCode locale) {
        return locale == LocaleCode.RU ? "Подписки" : "Subscriptions";
    }

    public static String sheetSummary(LocaleCode locale) {
        return locale == LocaleCode.RU ? "Сводка" : "Summary";
    }

    public static String title(LocaleCode locale) {
        return locale == LocaleCode.RU ? "ПодписOFF — подписки" : "SubOFF — subscriptions";
    }

    public static String exportedAt(LocaleCode locale) {
        return locale == LocaleCode.RU ? "Выгрузка:" : "Exported:";
    }

    public static String[] columns(LocaleCode locale) {
        if (locale == LocaleCode.RU) {
            return new String[] {
                "№", "Название", "Категория", "Сумма", "Валюта", "Период",
                "В месяц", "Следующее списание", "Активна", "Комментарий", "Ссылка"
            };
        }
        return new String[] {
            "ID", "Name", "Category", "Amount", "Currency", "Period",
            "Per month", "Next billing", "Active", "Note", "Link"
        };
    }

    public static String period(BillingPeriod period, LocaleCode locale) {
        if (period == BillingPeriod.YEARLY) {
            return locale == LocaleCode.RU ? "Каждый год" : "Yearly";
        }
        return locale == LocaleCode.RU ? "Каждый месяц" : "Monthly";
    }

    public static String active(boolean active, LocaleCode locale) {
        if (locale == LocaleCode.RU) {
            return active ? "Да" : "Нет";
        }
        return active ? "Yes" : "No";
    }

    public static String activeYes(LocaleCode locale) {
        return locale == LocaleCode.RU ? "Да" : "Yes";
    }

    public static String summaryTitle(LocaleCode locale) {
        return locale == LocaleCode.RU ? "Итоги (только активные)" : "Totals (active only)";
    }

    public static String summaryCount(LocaleCode locale) {
        return locale == LocaleCode.RU ? "Подписок" : "Subscriptions";
    }

    public static String summaryMonthly(LocaleCode locale, String currency) {
        return locale == LocaleCode.RU
            ? "В месяц, " + currency
            : "Monthly, " + currency;
    }

    public static String summaryByCategory(LocaleCode locale) {
        return locale == LocaleCode.RU ? "По категориям (в месяц)" : "By category (monthly)";
    }

    public static String categoryHeader(LocaleCode locale) {
        return locale == LocaleCode.RU ? "Категория" : "Category";
    }

    public static String totalHeader(LocaleCode locale) {
        return locale == LocaleCode.RU ? "Сумма в месяц" : "Monthly total";
    }

    public static String summaryApproxRub(LocaleCode locale) {
        return locale == LocaleCode.RU
            ? "≈ Всего в месяц, ₽ (оценка)"
            : "≈ Monthly total, ₽ (approx.)";
    }

    public static String summaryByCurrency(LocaleCode locale) {
        return locale == LocaleCode.RU ? "Итого по валютам" : "Totals by currency";
    }
}
