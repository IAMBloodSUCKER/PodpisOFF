package com.podpisoff.user;

import com.podpisoff.common.ApiException;
import com.podpisoff.reminder.ReminderRepeat;
import com.podpisoff.subscription.Subscription;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PlanAccessService {

    public static final int FREE_RECURRING_TRIAL_DAYS = 30;
    public static final String FREE_CURRENCY = "RUB";

    public Plan effectivePlan(User user) {
        if (user.getPlan() != Plan.PRO) {
            return Plan.FREE;
        }
        LocalDateTime expiresAt = user.getPlanExpiresAt();
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            return Plan.FREE;
        }
        return Plan.PRO;
    }

    public boolean canUseForeignCurrency(User user) {
        return effectivePlan(user) == Plan.PRO;
    }

    public boolean freeRecurringTrialActive(User user) {
        Instant trialEnd = user.getCreatedAt().plus(FREE_RECURRING_TRIAL_DAYS, ChronoUnit.DAYS);
        return Instant.now().isBefore(trialEnd);
    }

    public boolean canUseRecurringReminders(User user) {
        if (effectivePlan(user) == Plan.PRO) {
            return true;
        }
        return freeRecurringTrialActive(user);
    }

    public void validateSubscriptionCurrency(User user, String currency, String existingCurrency) {
        if (canUseForeignCurrency(user)) {
            return;
        }
        String normalized = currency == null ? "" : currency.trim().toUpperCase();
        if (FREE_CURRENCY.equals(normalized)) {
            return;
        }
        if (existingCurrency != null && normalized.equals(existingCurrency.trim().toUpperCase())) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Foreign currencies are available only for PRO plan");
    }

    public void validateReminderRepeat(User user, ReminderRepeat repeat, ReminderRepeat previousRepeat) {
        if (repeat == null || repeat == ReminderRepeat.ONCE) {
            return;
        }
        if (canUseRecurringReminders(user)) {
            return;
        }
        if (previousRepeat != null && previousRepeat != ReminderRepeat.ONCE) {
            return;
        }
        throw new ApiException(
            HttpStatus.FORBIDDEN,
            "Recurring reminders on Free are available only during the first month"
        );
    }

    public Set<Long> includedSubscriptionIds(User user, List<Subscription> allSubscriptions) {
        if (effectivePlan(user) == Plan.PRO) {
            return allSubscriptions.stream().map(Subscription::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return allSubscriptions.stream()
            .filter(Subscription::isActive)
            .sorted(Comparator.comparing(Subscription::getCreatedAt).thenComparing(Subscription::getId))
            .limit(PlanLimits.FREE_SUBSCRIPTION_LIMIT)
            .map(Subscription::getId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public List<Subscription> filterIncludedSubscriptions(User user, List<Subscription> subscriptions, Set<Long> includedIds) {
        if (effectivePlan(user) == Plan.PRO) {
            return subscriptions;
        }
        return subscriptions.stream().filter(subscription -> includedIds.contains(subscription.getId())).toList();
    }
}
