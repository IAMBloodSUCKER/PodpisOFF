package com.podpisoff.telegram;

import com.podpisoff.dashboard.DashboardAnalyticsResponse;
import com.podpisoff.dashboard.DashboardSummaryResponse;
import com.podpisoff.subscription.BillingPeriod;
import com.podpisoff.subscription.SubscriptionResponse;
import com.podpisoff.user.LocaleCode;
import com.podpisoff.user.Plan;
import com.podpisoff.user.PlanLimits;
import com.podpisoff.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

final class TelegramBotReplyFormatter {

    private static final int MAX_SUBSCRIPTIONS = 20;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private TelegramBotReplyFormatter() {
    }

    static String home(User user, String siteUrl, boolean isPro) {
        boolean russian = isRussian(user);
        Plan effectivePlan = isPro ? Plan.PRO : Plan.FREE;
        if (russian) {
            return TelegramHtml.bold("📋 ПодписOFF") + "\n\n"
                + "✅ Telegram подключён к аккаунту " + TelegramHtml.bold(user.getUsername()) + ".\n"
                + "🔔 Будем присылать напоминания о списаниях и важные уведомления.\n\n"
                + TelegramBotMessages.accountBlock(user, true, effectivePlan)
                + (isPro
                    ? "\n\n⭐ PRO: аналитика, экспорт Excel и сводка по всем подпискам."
                    : "\n\n🆓 Бесплатный тариф: до " + PlanLimits.FREE_SUBSCRIPTION_LIMIT + " подписок в сводке.")
                + "\n\n👇 Выберите раздел кнопками ниже.\n"
                + "🌐 Добавить и изменить подписки — на сайте.";
        }
        return TelegramHtml.bold("📋 PodpisOFF") + "\n\n"
            + "✅ Telegram is linked to account " + TelegramHtml.bold(user.getUsername()) + ".\n"
            + "🔔 We will send billing reminders and important alerts.\n\n"
            + TelegramBotMessages.accountBlock(user, false, effectivePlan)
            + (isPro
                ? "\n\n⭐ PRO: analytics, Excel export, and full subscription summary."
                : "\n\n🆓 Free plan: up to " + PlanLimits.FREE_SUBSCRIPTION_LIMIT + " subscriptions in summary.")
            + "\n\n👇 Choose a section with the buttons below.\n"
            + "🌐 Add and edit subscriptions on the site.";
    }

    static String home(User user, String siteUrl) {
        return home(user, siteUrl, false);
    }

    static String help(User user, String siteUrl) {
        return home(user, siteUrl);
    }

    static String subscriptions(User user, List<SubscriptionResponse> subscriptions) {
        return subscriptions(user, subscriptions, true, subscriptions.size());
    }

    static String subscriptions(User user, List<SubscriptionResponse> subscriptions, boolean isPro, int includedCount) {
        boolean russian = isRussian(user);
        if (subscriptions.isEmpty()) {
            return russian
                ? "📭 Подписок пока нет.\n\nДобавьте первую на сайте 🌐"
                : "📭 No subscriptions yet.\n\nAdd one on the site 🌐";
        }

        long activeCount = subscriptions.stream().filter(SubscriptionResponse::active).count();
        String header = russian
            ? TelegramHtml.bold("📋 Подписки") + " (" + activeCount + " активных из " + subscriptions.size() + ")"
            : TelegramHtml.bold("📋 Subscriptions") + " (" + activeCount + " active of " + subscriptions.size() + ")";

        StringBuilder message = new StringBuilder(header).append('\n');
        int shown = 0;
        for (SubscriptionResponse subscription : subscriptions) {
            if (shown >= MAX_SUBSCRIPTIONS) {
                message.append(russian ? "\n\n…и ещё на сайте 🌐" : "\n\n…and more on the site 🌐");
                break;
            }
            message.append('\n').append(formatSubscriptionLine(subscription, russian));
            shown++;
        }
        if (!isPro && activeCount > includedCount) {
            message.append(russian
                ? "\n\nℹ️ В сводке учитываются " + includedCount + " старейшие активные подписки.\n⭐ PRO — без ограничений."
                : "\n\nℹ️ Summary includes " + includedCount + " oldest active subscriptions.\n⭐ PRO removes this limit.");
        }
        return message.toString().strip();
    }

    static String upcoming(User user, List<SubscriptionResponse> upcoming) {
        boolean russian = isRussian(user);
        if (upcoming.isEmpty()) {
            return russian
                ? "✨ В ближайшие 31 день активных списаний нет."
                : "✨ No active charges in the next 31 days.";
        }

        String header = russian ? TelegramHtml.bold("📅 Ближайшие списания") : TelegramHtml.bold("📅 Upcoming charges");
        StringBuilder message = new StringBuilder(header);
        for (SubscriptionResponse subscription : upcoming) {
            message.append('\n').append(formatUpcomingLine(subscription, russian));
        }
        return message.toString().strip();
    }

    static String summary(User user, DashboardSummaryResponse summary) {
        boolean russian = isRussian(user);
        String monthLabel = monthName(summary.selectedMonth(), russian) + " " + summary.selectedYear();
        StringBuilder message = new StringBuilder();
        message.append(russian ? TelegramHtml.bold("📊 Сводка") : TelegramHtml.bold("📊 Summary"))
            .append(" (").append(TelegramHtml.escape(monthLabel)).append("):\n");

        appendTotals(
            message,
            russian ? "В месяц" : "Monthly",
            summary.monthlyByCurrency(),
            russian
        );
        appendTotals(
            message,
            russian ? "В год" : "Yearly",
            summary.yearlyByCurrency(),
            russian
        );
        appendTotals(
            message,
            russian ? "Потрачено в " + monthName(summary.selectedMonth(), russian).toLowerCase() : "Spent in " + monthNameEn(summary.selectedMonth()),
            summary.monthSpendByCurrency(),
            russian
        );

        if (!summary.byCategory().isEmpty()) {
            message.append('\n').append(russian ? "🏷 По категориям (в месяц):" : "🏷 By category (monthly):");
            summary.byCategory().forEach((category, totals) -> {
                message.append('\n').append("• ").append(TelegramHtml.escape(category)).append(": ");
                message.append(TelegramHtml.escape(formatCurrencyMap(totals)));
            });
        }
        return message.toString().strip();
    }

    static String analytics(User user, DashboardAnalyticsResponse analytics) {
        boolean russian = isRussian(user);
        StringBuilder message = new StringBuilder(russian ? TelegramHtml.bold("📈 Аналитика PRO") : TelegramHtml.bold("📈 PRO analytics"));

        if (!analytics.monthlyTrend().isEmpty()) {
            message.append('\n').append(russian ? "\n📉 Тренд за 6 мес.:" : "\n📉 6-month trend:");
            for (DashboardAnalyticsResponse.MonthSpendPoint point : analytics.monthlyTrend()) {
                message.append('\n').append("• ")
                    .append(shortMonth(point.month(), russian))
                    .append(' ')
                    .append(point.year())
                    .append(": ")
                    .append(TelegramHtml.escape(formatCurrencyMap(point.spendByCurrency())));
            }
        }

        if (!analytics.topByMonthlyCost().isEmpty()) {
            message.append('\n').append(russian ? "\n🏆 Топ подписок (в месяц):" : "\n🏆 Top subscriptions (monthly):");
            int rank = 1;
            for (DashboardAnalyticsResponse.TopSubscriptionItem item : analytics.topByMonthlyCost()) {
                if (rank > 8) {
                    break;
                }
                message.append('\n').append(rank++).append(". ")
                    .append(TelegramHtml.bold(item.title()))
                    .append(" — ")
                    .append(TelegramHtml.escape(formatMoney(item.monthlyBurn(), item.currency())));
            }
        }

        if (!analytics.byCategory().isEmpty()) {
            message.append('\n').append(russian ? "\n🏷 По категориям (в месяц):" : "\n🏷 By category (monthly):");
            analytics.byCategory().forEach((category, totals) -> message.append('\n')
                .append("• ")
                .append(TelegramHtml.escape(category))
                .append(": ")
                .append(TelegramHtml.escape(formatCurrencyMap(totals))));
        }
        return message.toString().strip();
    }

    static String proUpsell(User user, TelegramMenuAction feature, String billingUrl) {
        boolean russian = isRussian(user);
        String featureName = switch (feature) {
            case ANALYTICS -> russian ? "Аналитика" : "Analytics";
            case EXPORT -> russian ? "Экспорт Excel" : "Excel export";
            default -> russian ? "PRO-функция" : "PRO feature";
        };
        if (russian) {
            return "⭐ " + TelegramHtml.bold(featureName + " — только PRO") + "\n\n"
                + "PRO открывает:\n"
                + "📈 аналитику и тренды за 6 месяцев;\n"
                + "📤 экспорт подписок в Excel;\n"
                + "♾ безлимит подписок и все валюты;\n"
                + "📊 сводку по любому месяцу.\n\n"
                + "👉 Оформить: " + TelegramHtml.escape(billingUrl);
        }
        return "⭐ " + TelegramHtml.bold(featureName + " — PRO only") + "\n\n"
            + "PRO unlocks:\n"
            + "📈 analytics and 6-month trends;\n"
            + "📤 Excel export;\n"
            + "♾ unlimited subscriptions and all currencies;\n"
            + "📊 summary for any month.\n\n"
            + "👉 Upgrade: " + TelegramHtml.escape(billingUrl);
    }

    static String plan(User user, boolean isPro, int totalSubscriptions, int includedCount) {
        boolean russian = isRussian(user);
        StringBuilder message = new StringBuilder(
            russian ? TelegramHtml.bold("👤 Аккаунт") + "\n\n" : TelegramHtml.bold("👤 Account") + "\n\n"
        );
        message.append(
            TelegramBotMessages.accountBlock(
                user,
                russian,
                isPro ? Plan.PRO : Plan.FREE
            )
        );
        message.append('\n').append(russian ? "\n📋 Подписок: " : "\n📋 Subscriptions: ").append(totalSubscriptions);
        if (!isPro) {
            message.append(russian
                ? " (в сводке " + includedCount + " из " + PlanLimits.FREE_SUBSCRIPTION_LIMIT + ")"
                : " (" + includedCount + " of " + PlanLimits.FREE_SUBSCRIPTION_LIMIT + " in summary)");
            message.append(russian
                ? "\n\n⭐ Оформите PRO в настройках сайта для аналитики, экспорта и без лимитов."
                : "\n\n⭐ Get PRO in site settings for analytics, export, and no limits.");
        } else {
            message.append(russian
                ? "\n\n✅ PRO активен: аналитика, экспорт Excel, все подписки и валюты."
                : "\n\n✅ PRO active: analytics, Excel export, all subscriptions and currencies.");
        }
        return message.toString();
    }

    static String plan(User user) {
        return TelegramBotMessages.accountBlock(user, user.getLocale() != LocaleCode.EN);
    }

    static String unknownCommand(User user) {
        return isRussian(user)
            ? "Неизвестная команда. /help — список команд."
            : "Unknown command. /help — command list.";
    }

    private static void appendTotals(StringBuilder message, String label, Map<String, BigDecimal> totals, boolean russian) {
        String prefix = switch (label) {
            case "В месяц", "Monthly" -> "💰 ";
            case "В год", "Yearly" -> "📆 ";
            default -> "💸 ";
        };
        if (totals == null || totals.isEmpty()) {
            message.append('\n').append(prefix).append(TelegramHtml.bold(label)).append(": 0");
            return;
        }
        message.append('\n').append(prefix).append(TelegramHtml.bold(label)).append(": ").append(TelegramHtml.escape(formatCurrencyMap(totals)));
    }

    private static String formatSubscriptionLine(SubscriptionResponse subscription, boolean russian) {
        String status = subscription.active()
            ? (russian ? "активна" : "active")
            : (russian ? "пауза" : "paused");
        String statusEmoji = subscription.active() ? "🟢" : "⏸";
        return statusEmoji + " " + TelegramHtml.bold(subscription.title())
            + " — " + TelegramHtml.escape(formatMoney(subscription.amount(), subscription.currency()))
            + ", " + TelegramHtml.escape(formatDate(subscription.nextBillingDate()))
            + " (" + TelegramHtml.escape(periodLabel(subscription.billingPeriod(), russian) + ", " + status) + ")";
    }

    private static String formatUpcomingLine(SubscriptionResponse subscription, boolean russian) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), subscription.nextBillingDate());
        String when = days <= 0
            ? (russian ? "сегодня" : "today")
            : (russian ? "через " + days + " дн." : "in " + days + " days");
        String whenEmoji = days <= 0 ? "🔥" : "⏳";
        return whenEmoji + " " + TelegramHtml.bold(subscription.title())
            + " — " + TelegramHtml.escape(formatMoney(subscription.amount(), subscription.currency()))
            + ", " + TelegramHtml.escape(formatDate(subscription.nextBillingDate()))
            + " (" + TelegramHtml.escape(when) + ")";
    }

    private static String formatCurrencyMap(Map<String, BigDecimal> totals) {
        StringBuilder line = new StringBuilder();
        totals.forEach((currency, amount) -> {
            if (!line.isEmpty()) {
                line.append(", ");
            }
            line.append(formatMoney(amount, currency));
        });
        return line.isEmpty() ? "0" : line.toString();
    }

    private static String formatMoney(BigDecimal amount, String currency) {
        if (amount == null) {
            return "0 " + (currency == null ? "" : currency.trim());
        }
        return amount.stripTrailingZeros().toPlainString() + " " + (currency == null ? "" : currency.trim());
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "—" : date.format(DATE);
    }

    private static String periodLabel(BillingPeriod period, boolean russian) {
        if (period == BillingPeriod.YEARLY) {
            return russian ? "год" : "year";
        }
        return russian ? "мес" : "mo";
    }

    private static String monthName(int month, boolean russian) {
        if (!russian) {
            return monthNameEn(month);
        }
        return switch (month) {
            case 1 -> "Январь";
            case 2 -> "Февраль";
            case 3 -> "Март";
            case 4 -> "Апрель";
            case 5 -> "Май";
            case 6 -> "Июнь";
            case 7 -> "Июль";
            case 8 -> "Август";
            case 9 -> "Сентябрь";
            case 10 -> "Октябрь";
            case 11 -> "Ноябрь";
            case 12 -> "Декабрь";
            default -> String.valueOf(month);
        };
    }

    private static String shortMonth(int month, boolean russian) {
        if (!russian) {
            return monthNameEn(month).substring(0, 3);
        }
        return switch (month) {
            case 1 -> "Янв";
            case 2 -> "Фев";
            case 3 -> "Мар";
            case 4 -> "Апр";
            case 5 -> "Май";
            case 6 -> "Июн";
            case 7 -> "Июл";
            case 8 -> "Авг";
            case 9 -> "Сен";
            case 10 -> "Окт";
            case 11 -> "Ноя";
            case 12 -> "Дек";
            default -> String.valueOf(month);
        };
    }

    private static String monthNameEn(int month) {
        return switch (month) {
            case 1 -> "January";
            case 2 -> "February";
            case 3 -> "March";
            case 4 -> "April";
            case 5 -> "May";
            case 6 -> "June";
            case 7 -> "July";
            case 8 -> "August";
            case 9 -> "September";
            case 10 -> "October";
            case 11 -> "November";
            case 12 -> "December";
            default -> String.valueOf(month);
        };
    }

    private static boolean isRussian(User user) {
        return user.getLocale() != LocaleCode.EN;
    }

    private static String trimSiteUrl(String siteUrl) {
        if (siteUrl == null || siteUrl.isBlank()) {
            return "http://localhost:3000";
        }
        return siteUrl.replaceAll("/$", "");
    }
}
