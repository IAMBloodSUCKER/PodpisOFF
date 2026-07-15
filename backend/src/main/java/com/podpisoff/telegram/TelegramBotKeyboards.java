package com.podpisoff.telegram;

import com.podpisoff.user.LocaleCode;
import com.podpisoff.user.User;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TelegramBotKeyboards {

    private TelegramBotKeyboards() {
    }

    static List<List<Map<String, String>>> linkedMenu(User user, boolean isPro) {
        boolean ru = isRussian(user);
        List<List<Map<String, String>>> rows = new ArrayList<>();
        rows.add(row(
            button(ru ? "Подписки" : "Subscriptions", TelegramMenuAction.SUBS),
            button(ru ? "Ближайшие" : "Upcoming", TelegramMenuAction.UPCOMING)
        ));
        rows.add(row(
            button(ru ? "Сводка" : "Summary", TelegramMenuAction.SUMMARY),
            button(ru ? "Аккаунт" : "Account", TelegramMenuAction.PLAN)
        ));
        if (isPro) {
            rows.add(row(
                button(ru ? "Аналитика" : "Analytics", TelegramMenuAction.ANALYTICS),
                button(ru ? "Экспорт Excel" : "Export Excel", TelegramMenuAction.EXPORT)
            ));
        }
        rows.add(row(button(ru ? "Отключить уведомления" : "Turn off notifications", TelegramMenuAction.DISABLE)));
        return rows;
    }

    static List<List<Map<String, String>>> forScreen(User user, TelegramMenuAction action, boolean isPro) {
        return switch (action) {
            case HOME -> linkedMenu(user, isPro);
            case SUMMARY -> isPro ? summaryNavigationMenu(user) : backToMenu(user);
            default -> backToMenu(user);
        };
    }

    static List<List<Map<String, String>>> summaryNavigationMenu(User user) {
        boolean ru = isRussian(user);
        return List.of(
            row(
                button(ru ? "← Месяц" : "← Month", TelegramMenuAction.SUMMARY_PREV),
                button(ru ? "Месяц →" : "Month →", TelegramMenuAction.SUMMARY_NEXT)
            ),
            row(button(ru ? "← Меню" : "← Menu", TelegramMenuAction.HOME))
        );
    }

    static List<List<Map<String, String>>> proUpsellMenu(User user, String billingUrl) {
        boolean ru = isRussian(user);
        return List.of(
            row(urlButton(ru ? "Оформить PRO" : "Get PRO", billingUrl)),
            row(button(ru ? "← Меню" : "← Menu", TelegramMenuAction.HOME))
        );
    }

    static List<List<Map<String, String>>> backToMenu(User user) {
        boolean ru = isRussian(user);
        return List.of(row(button(ru ? "← Меню" : "← Menu", TelegramMenuAction.HOME)));
    }

    static List<List<Map<String, String>>> unlinkedMenu(String siteUrl) {
        return List.of(row(urlButton("Открыть сайт", siteUrl)));
    }

    static List<List<Map<String, String>>> disconnectedMenu(String siteUrl) {
        return List.of(row(urlButton("Подключить на сайте", siteUrl)));
    }

    private static Map<String, String> button(String text, TelegramMenuAction action) {
        Map<String, String> button = new LinkedHashMap<>();
        button.put("text", text);
        button.put("callback_data", action.callbackData());
        return button;
    }

    private static Map<String, String> urlButton(String text, String url) {
        Map<String, String> button = new LinkedHashMap<>();
        button.put("text", text);
        button.put("url", url);
        return button;
    }

    @SafeVarargs
    private static List<Map<String, String>> row(Map<String, String>... buttons) {
        List<Map<String, String>> row = new ArrayList<>();
        for (Map<String, String> button : buttons) {
            row.add(button);
        }
        return row;
    }

    private static boolean isRussian(User user) {
        return user == null || user.getLocale() != LocaleCode.EN;
    }
}
