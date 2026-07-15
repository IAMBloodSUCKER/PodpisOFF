package com.podpisoff.telegram;



import com.podpisoff.user.LocaleCode;

import com.podpisoff.user.Plan;

import com.podpisoff.user.PlanLimits;

import com.podpisoff.user.User;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;



final class TelegramBotMessages {



    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");



    private TelegramBotMessages() {

    }



    static String linkSuccess(User user) {

        boolean russian = user.getLocale() == LocaleCode.RU;

        return russian ? linkSuccessRu(user) : linkSuccessEn(user);

    }



    static String linkedAccountStatus(User user) {

        boolean russian = user.getLocale() == LocaleCode.RU;

        return russian ? linkedAccountStatusRu(user) : linkedAccountStatusEn(user);

    }



    static String unlinkedWelcome(String botUsername) {

        String botLink = botUsername != null && !botUsername.isBlank()

            ? "t.me/" + botUsername

            : null;

        StringBuilder message = new StringBuilder();

        message.append(TelegramHtml.bold("👋 Привет! Я бот ПодписOFF.")).append("\n\n");

        message.append("Чтобы подключить уведомления, откройте ").append(TelegramHtml.bold("Настройки"))

            .append(" на сайте, примите условия и нажмите «Подключить Telegram».\n");

        message.append("Мы пришлём ссылку с QR-кодом — откройте её и нажмите ").append(TelegramHtml.bold("Start")).append(".\n\n");

        if (botLink != null) {

            message.append("🔗 Подключение только через ссылку из настроек:\n").append(TelegramHtml.escape(botLink)).append("\n\n");

        }

        message.append("👇 Нажмите Start — откроется меню с кнопками.");

        return message.toString();

    }



    static String invalidLink() {

        return TelegramHtml.bold("⚠️ Ссылка устарела или уже использована.") + "\n\n"

            + "Откройте настройки на сайте и подключите Telegram заново.";

    }



    static String disconnected() {

        return TelegramHtml.bold("🔕 Уведомления в Telegram отключены.") + "\n\n"

            + "Снова подключить можно в настройках на сайте.";

    }



    private static String linkSuccessRu(User user) {

        return TelegramHtml.bold("🎉 Telegram подключён!") + "\n\n"

            + accountBlockRu(user)

            + "\n\nБудем присылать:\n"

            + "🔔 напоминания о списаниях;\n"

            + "💬 ответы поддержки и сообщения администратора;\n"

            + "💳 уведомления о тарифе.\n\n"

            + "👇 Нажмите Start — откроется меню с кнопками.\n\n"

            + "Отключить: кнопка в меню или в настройках на сайте.";

    }



    private static String linkSuccessEn(User user) {

        return TelegramHtml.bold("🎉 Telegram connected!") + "\n\n"

            + accountBlockEn(user)

            + "\n\nWe will send:\n"

            + "🔔 billing reminders;\n"

            + "💬 support replies and admin messages;\n"

            + "💳 plan notifications.\n\n"

            + "👇 Tap Start to open the button menu.\n\n"

            + "To turn off: use the menu button or site settings.";

    }



    private static String linkedAccountStatusRu(User user) {

        return TelegramHtml.bold("✅ Telegram подключён") + "\n\n"

            + accountBlockRu(user)

            + "\n\n🔔 Уведомления включены. Нажмите Start для меню.";

    }



    private static String linkedAccountStatusEn(User user) {

        return TelegramHtml.bold("✅ Telegram connected") + "\n\n"

            + accountBlockEn(user)

            + "\n\n🔔 Notifications are on. Tap Start for the menu.";

    }



    private static String accountBlockRu(User user) {

        return accountBlock(user, true);

    }



    private static String accountBlockEn(User user) {

        return accountBlock(user, false);

    }



    static String accountBlock(User user, boolean russian) {

        return accountBlock(user, russian, null);

    }



    static String accountBlock(User user, boolean russian, Plan effectivePlan) {

        Plan plan = effectivePlan != null ? effectivePlan : (user.getPlan() != null ? user.getPlan() : Plan.FREE);

        return TelegramHtml.labelLine("👤", russian ? "Логин" : "Username", user.getUsername())

            + "\n"

            + TelegramHtml.labelLine("✉️", russian ? "Почта" : "Email", emailLabel(user, russian))

            + "\n"

            + TelegramHtml.labelLine("💳", russian ? "Тариф" : "Plan", planLabel(user, plan, russian));

    }



    private static String emailLabel(User user, boolean russian) {

        String email = user.getEmail();

        if (email == null || email.isBlank()) {

            return russian ? "не указана" : "not set";

        }

        return email.trim();

    }



    private static String planLabel(User user, Plan effectivePlan, boolean russian) {

        if (effectivePlan == Plan.PRO) {

            LocalDateTime expiresAt = user.getPlanExpiresAt();

            if (expiresAt != null) {

                String until = expiresAt.toLocalDate().format(DATE);

                return russian ? "⭐ PRO (до " + until + ")" : "⭐ PRO (until " + until + ")";

            }

            return "⭐ PRO";

        }

        if (user.getPlan() == Plan.PRO) {

            return russian ? "🆓 Бесплатный (PRO истёк)" : "🆓 Free (PRO expired)";

        }

        return russian

            ? "🆓 Бесплатный (до " + PlanLimits.FREE_SUBSCRIPTION_LIMIT + " подписок, только RUB)"

            : "🆓 Free (up to " + PlanLimits.FREE_SUBSCRIPTION_LIMIT + " subs, RUB only)";

    }

}

