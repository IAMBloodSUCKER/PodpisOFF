package com.podpisoff.subscription;

import com.podpisoff.notification.NotificationService;
import com.podpisoff.notification.NotificationType;
import com.podpisoff.user.LocaleCode;
import com.podpisoff.user.User;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionNotificationService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final NotificationService notificationService;

    public SubscriptionNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void notifyCreated(User user, SubscriptionResponse subscription) {
        notify(user, subscription, NotificationType.SUBSCRIPTION_ADDED);
    }

    public void notifyUpdated(User user, SubscriptionResponse subscription) {
        notify(user, subscription, NotificationType.SUBSCRIPTION_UPDATED);
    }

    public void notifyDeleted(User user, SubscriptionResponse subscription) {
        notify(user, subscription, NotificationType.SUBSCRIPTION_DELETED);
    }

    private void notify(User user, SubscriptionResponse subscription, NotificationType type) {
        if (user == null || subscription == null) {
            return;
        }
        boolean russian = user.getLocale() != LocaleCode.EN;
        String title = switch (type) {
            case SUBSCRIPTION_ADDED -> russian ? "✅ Подписка добавлена" : "✅ Subscription added";
            case SUBSCRIPTION_UPDATED -> russian ? "✏️ Подписка изменена" : "✏️ Subscription updated";
            case SUBSCRIPTION_DELETED -> russian ? "🗑️ Подписка удалена" : "🗑️ Subscription deleted";
            default -> russian ? "📋 Подписка" : "📋 Subscription";
        };
        boolean deleted = type == NotificationType.SUBSCRIPTION_DELETED;
        String body = formatBody(subscription, russian, deleted);
        notificationService.create(user, type, title, body, subscription.id());
    }

    private static String formatBody(SubscriptionResponse subscription, boolean russian, boolean deleted) {
        if (deleted) {
            return "📌 " + subscription.title();
        }
        String amount = subscription.amount().stripTrailingZeros().toPlainString()
            + " " + subscription.currency();
        String date = subscription.nextBillingDate() == null
            ? "—"
            : subscription.nextBillingDate().format(DATE);
        String status = subscription.active()
            ? (russian ? "активна" : "active")
            : (russian ? "пауза" : "paused");
        String statusEmoji = subscription.active() ? "🟢" : "⏸";
        return statusEmoji + " " + subscription.title() + "\n💰 " + amount + " · 📅 " + date + " · " + status;
    }
}
