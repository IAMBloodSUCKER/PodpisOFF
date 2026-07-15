package com.podpisoff.admin;

import com.podpisoff.auth.LoginEvent;
import com.podpisoff.auth.LoginEventRepository;
import com.podpisoff.feedback.Feedback;
import com.podpisoff.feedback.FeedbackRepository;
import com.podpisoff.push.PushSubscriptionRepository;
import com.podpisoff.reminder.Reminder;
import com.podpisoff.reminder.ReminderRepeat;
import com.podpisoff.reminder.ReminderRepository;
import com.podpisoff.subscription.BillingPeriod;
import com.podpisoff.subscription.Subscription;
import com.podpisoff.subscription.SubscriptionRepository;
import com.podpisoff.user.LocaleCode;
import com.podpisoff.user.Plan;
import com.podpisoff.user.PlanAccessService;
import com.podpisoff.user.User;
import com.podpisoff.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AdminMetricsService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ReminderRepository reminderRepository;
    private final LoginEventRepository loginEventRepository;
    private final FeedbackRepository feedbackRepository;
    private final PlanAccessService planAccessService;
    private final PushSubscriptionRepository pushSubscriptionRepository;

    public AdminMetricsService(UserRepository userRepository,
                               SubscriptionRepository subscriptionRepository,
                               ReminderRepository reminderRepository,
                               LoginEventRepository loginEventRepository,
                               FeedbackRepository feedbackRepository,
                               PlanAccessService planAccessService,
                               PushSubscriptionRepository pushSubscriptionRepository) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.reminderRepository = reminderRepository;
        this.loginEventRepository = loginEventRepository;
        this.feedbackRepository = feedbackRepository;
        this.planAccessService = planAccessService;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
    }

    public AdminMetricsResponse metrics() {
        Instant now = Instant.now();
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant weekAgo = now.minus(7, ChronoUnit.DAYS);
        Instant monthAgo = now.minus(30, ChronoUnit.DAYS);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        List<User> users = userRepository.findAll();
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        List<Reminder> reminders = reminderRepository.findAll();
        List<LoginEvent> loginEvents = loginEventRepository.findAll();
        List<Feedback> feedbackItems = feedbackRepository.findAll();

        long totalUsers = users.size();
        long effectiveProUsers = users.stream().filter(user -> planAccessService.effectivePlan(user) == Plan.PRO).count();
        long effectiveFreeUsers = totalUsers - effectiveProUsers;
        long blockedUsers = users.stream().filter(User::isCurrentlyBlocked).count();

        long activeSubscriptions = subscriptions.stream().filter(Subscription::isActive).count();
        long pausedSubscriptions = subscriptions.stream()
            .filter(subscription -> !subscription.isActive() && !subscription.getNextBillingDate().isBefore(today))
            .count();
        long offSubscriptions = subscriptions.stream()
            .filter(subscription -> !subscription.isActive() && subscription.getNextBillingDate().isBefore(today))
            .count();

        long recurringReminders = reminders.stream()
            .filter(reminder -> reminder.getRepeatType() != ReminderRepeat.ONCE)
            .count();

        long usersWithEmailNotify = users.stream().filter(User::isEmailNotificationsEnabled).count();
        long usersWithTelegramNotify = users.stream().filter(User::isTelegramNotificationsEnabled).count();

        return new AdminMetricsResponse(
            totalUsers,
            users.stream().filter(user -> user.getCreatedAt().isAfter(startOfDay)).count(),
            users.stream().filter(user -> user.getCreatedAt().isAfter(weekAgo)).count(),
            users.stream().filter(user -> user.getCreatedAt().isAfter(monthAgo)).count(),
            users.stream().filter(user -> user.getPlan() == Plan.FREE).count(),
            users.stream().filter(user -> user.getPlan() == Plan.PRO).count(),
            users.stream().filter(user -> user.getPlan() == Plan.PRO
                && user.getPlanExpiresAt() != null
                && user.getPlanExpiresAt().isBefore(java.time.LocalDateTime.now())).count(),
            effectiveProUsers,
            effectiveFreeUsers,
            blockedUsers,
            subscriptions.size(),
            activeSubscriptions,
            pausedSubscriptions,
            offSubscriptions,
            reminders.size(),
            recurringReminders,
            loginEvents.stream().filter(event -> event.getLoggedInAt().isAfter(startOfDay)).count(),
            loginEvents.stream().filter(event -> event.getLoggedInAt().isAfter(weekAgo)).count(),
            loginEvents.stream().filter(event -> event.getLoggedInAt().isAfter(monthAgo)).count(),
            distinctUsersSince(loginEvents, startOfDay),
            distinctUsersSince(loginEvents, weekAgo),
            distinctUsersSince(loginEvents, monthAgo),
            feedbackItems.size(),
            feedbackItems.stream().filter(item -> !item.isReplied()).count(),
            feedbackItems.stream().filter(Feedback::isReplied).count(),
            pushSubscriptionRepository.count(),
            usersWithEmailNotify,
            usersWithTelegramNotify,
            averageSubscriptionsPerUser(totalUsers, subscriptions.size()),
            registrationsByDay(users, weekAgo),
            eventsByDay(loginEvents, weekAgo, LoginEvent::getLoggedInAt),
            uniqueLoginsByDay(loginEvents, weekAgo),
            subscriptionsCreatedByDay(subscriptions, weekAgo),
            feedbackByDay(feedbackItems, weekAgo),
            effectivePlanSlice(users),
            subscriptionStatusSlice(subscriptions, today),
            billingPeriodSlice(subscriptions),
            topCategories(subscriptions),
            currencySlice(subscriptions),
            localeSlice(users),
            usersBySubscriptionCountSlice(users, subscriptions),
            blockedUsersSlice(users),
            notificationChannelsSlice(usersWithEmailNotify, usersWithTelegramNotify, pushSubscriptionRepository.count()),
            reminderRepeatSlice(reminders)
        );
    }

    public List<AdminUserResponse> users(AdminUserListFilters filters) {
        String search = filters == null ? null : filters.search();
        return userRepository.findAll().stream()
            .filter(user -> matchesPlanFilter(user, filters == null ? null : filters.plan()))
            .filter(user -> matchesSearch(user, search))
            .filter(user -> matchesEmailStatus(user, filters == null ? null : filters.emailStatus()))
            .filter(user -> matchesTelegramStatus(user, filters == null ? null : filters.telegramStatus()))
            .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
            .map(this::toUserResponse)
            .toList();
    }

    public List<AdminUserResponse> users(String planFilter) {
        return users(AdminUserListFilters.of(planFilter, null, null, null));
    }

    private boolean matchesSearch(User user, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String query = search.trim().toLowerCase();
        String username = user.getUsername() == null ? "" : user.getUsername().toLowerCase();
        String email = user.getEmail() == null ? "" : user.getEmail().toLowerCase();
        return username.contains(query) || email.contains(query);
    }

    private boolean matchesEmailStatus(User user, String emailStatus) {
        if (emailStatus == null || emailStatus.isBlank() || "all".equalsIgnoreCase(emailStatus)) {
            return true;
        }
        boolean hasEmail = hasEmail(user);
        return switch (emailStatus.toLowerCase()) {
            case "set", "with" -> hasEmail;
            case "unset", "without" -> !hasEmail;
            case "notify_on" -> user.isEmailNotificationsEnabled() && hasEmail;
            case "notify_off" -> !user.isEmailNotificationsEnabled() || !hasEmail;
            default -> true;
        };
    }

    private boolean matchesTelegramStatus(User user, String telegramStatus) {
        if (telegramStatus == null || telegramStatus.isBlank() || "all".equalsIgnoreCase(telegramStatus)) {
            return true;
        }
        boolean linked = isTelegramLinked(user);
        return switch (telegramStatus.toLowerCase()) {
            case "connected", "linked", "with" -> linked;
            case "not_connected", "not_linked", "without" -> !linked;
            case "notify_on" -> user.isTelegramNotificationsEnabled() && linked;
            case "notify_off" -> !user.isTelegramNotificationsEnabled() || !linked;
            default -> true;
        };
    }

    private static boolean hasEmail(User user) {
        return user.getEmail() != null && !user.getEmail().isBlank();
    }

    private static boolean isTelegramLinked(User user) {
        return user.getTelegramChatId() != null && !user.getTelegramChatId().isBlank();
    }

    private boolean matchesPlanFilter(User user, String planFilter) {
        if (planFilter == null || planFilter.isBlank() || "all".equalsIgnoreCase(planFilter)) {
            return true;
        }
        Plan effective = planAccessService.effectivePlan(user);
        if ("pro".equalsIgnoreCase(planFilter)) {
            return effective == Plan.PRO;
        }
        if ("free".equalsIgnoreCase(planFilter)) {
            return effective == Plan.FREE;
        }
        return true;
    }

    public AdminUserResponse toUserResponse(User user) {
        return new AdminUserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.isEmailNotificationsEnabled(),
            isTelegramLinked(user),
            user.isTelegramNotificationsEnabled(),
            user.getPlan().name(),
            planAccessService.effectivePlan(user).name(),
            user.getPlanExpiresAt(),
            user.getCreatedAt(),
            subscriptionRepository.countByUserId(user.getId()),
            reminderRepository.countByUserId(user.getId()),
            user.isBlockedPermanently(),
            user.getBlockedUntil(),
            user.isCurrentlyBlocked()
        );
    }

    private long distinctUsersSince(List<LoginEvent> loginEvents, Instant since) {
        return loginEvents.stream()
            .filter(event -> event.getLoggedInAt().isAfter(since))
            .map(event -> event.getUser().getId())
            .distinct()
            .count();
    }

    private double averageSubscriptionsPerUser(long totalUsers, long totalSubscriptions) {
        if (totalUsers == 0) {
            return 0;
        }
        return (double) totalSubscriptions / totalUsers;
    }

    private List<DailyCount> registrationsByDay(List<User> users, Instant since) {
        Map<LocalDate, Long> counts = emptyLast7Days();
        users.stream()
            .filter(user -> user.getCreatedAt().isAfter(since))
            .forEach(user -> incrementDay(counts, user.getCreatedAt()));
        return toDailyCounts(counts);
    }

    private List<DailyCount> subscriptionsCreatedByDay(List<Subscription> subscriptions, Instant since) {
        Map<LocalDate, Long> counts = emptyLast7Days();
        subscriptions.stream()
            .filter(subscription -> subscription.getCreatedAt().isAfter(since))
            .forEach(subscription -> incrementDay(counts, subscription.getCreatedAt()));
        return toDailyCounts(counts);
    }

    private List<DailyCount> feedbackByDay(List<Feedback> feedbackItems, Instant since) {
        Map<LocalDate, Long> counts = emptyLast7Days();
        feedbackItems.stream()
            .filter(item -> item.getCreatedAt().isAfter(since))
            .forEach(item -> incrementDay(counts, item.getCreatedAt()));
        return toDailyCounts(counts);
    }

    private List<DailyCount> eventsByDay(List<LoginEvent> events, Instant since, java.util.function.Function<LoginEvent, Instant> extractor) {
        Map<LocalDate, Long> counts = emptyLast7Days();
        events.stream()
            .filter(event -> extractor.apply(event).isAfter(since))
            .forEach(event -> incrementDay(counts, extractor.apply(event)));
        return toDailyCounts(counts);
    }

    private List<DailyCount> uniqueLoginsByDay(List<LoginEvent> events, Instant since) {
        Map<LocalDate, Set<Long>> usersByDay = new LinkedHashMap<>();
        emptyLast7Days().keySet().forEach(day -> usersByDay.put(day, new HashSet<>()));
        events.stream()
            .filter(event -> event.getLoggedInAt().isAfter(since))
            .forEach(event -> {
                LocalDate day = event.getLoggedInAt().atZone(ZoneOffset.UTC).toLocalDate();
                usersByDay.computeIfAbsent(day, ignored -> new HashSet<>()).add(event.getUser().getId());
            });
        List<DailyCount> result = new ArrayList<>();
        usersByDay.forEach((day, userIds) -> result.add(new DailyCount(day.toString(), userIds.size())));
        return result;
    }

    private List<LabelCount> effectivePlanSlice(List<User> users) {
        long pro = users.stream().filter(user -> planAccessService.effectivePlan(user) == Plan.PRO).count();
        long free = users.size() - pro;
        return List.of(
            new LabelCount("effective_pro", pro),
            new LabelCount("effective_free", free)
        );
    }

    private List<LabelCount> subscriptionStatusSlice(List<Subscription> subscriptions, LocalDate today) {
        long active = 0;
        long paused = 0;
        long off = 0;
        for (Subscription subscription : subscriptions) {
            if (subscription.isActive()) {
                active++;
            } else if (!subscription.getNextBillingDate().isBefore(today)) {
                paused++;
            } else {
                off++;
            }
        }
        return List.of(
            new LabelCount("sub_active", active),
            new LabelCount("sub_paused", paused),
            new LabelCount("sub_off", off)
        );
    }

    private List<LabelCount> billingPeriodSlice(List<Subscription> subscriptions) {
        long monthly = subscriptions.stream().filter(sub -> sub.getBillingPeriod() == BillingPeriod.MONTHLY).count();
        long yearly = subscriptions.size() - monthly;
        return List.of(
            new LabelCount("billing_monthly", monthly),
            new LabelCount("billing_yearly", yearly)
        );
    }

    private List<LabelCount> topCategories(List<Subscription> subscriptions) {
        Map<String, Long> counts = new HashMap<>();
        subscriptions.forEach(subscription -> {
            String category = subscription.getCategory() == null ? "—" : subscription.getCategory().trim();
            if (category.isBlank()) {
                category = "—";
            }
            counts.merge(category, 1L, Long::sum);
        });
        return counts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
            .limit(8)
            .map(entry -> new LabelCount(entry.getKey(), entry.getValue()))
            .toList();
    }

    private List<LabelCount> currencySlice(List<Subscription> subscriptions) {
        Map<String, Long> counts = new HashMap<>();
        subscriptions.forEach(subscription -> {
            String currency = subscription.getCurrency() == null ? "RUB" : subscription.getCurrency().trim().toUpperCase();
            counts.merge(currency, 1L, Long::sum);
        });
        return counts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
            .limit(8)
            .map(entry -> new LabelCount(entry.getKey(), entry.getValue()))
            .toList();
    }

    private List<LabelCount> localeSlice(List<User> users) {
        long ru = users.stream().filter(user -> user.getLocale() == LocaleCode.RU).count();
        long en = users.size() - ru;
        return List.of(
            new LabelCount("locale_ru", ru),
            new LabelCount("locale_en", en)
        );
    }

    private List<LabelCount> usersBySubscriptionCountSlice(List<User> users, List<Subscription> subscriptions) {
        Map<Long, Long> subsPerUser = subscriptions.stream()
            .collect(Collectors.groupingBy(subscription -> subscription.getUser().getId(), Collectors.counting()));
        long zero = 0;
        long oneToThree = 0;
        long fourPlus = 0;
        for (User user : users) {
            long count = subsPerUser.getOrDefault(user.getId(), 0L);
            if (count == 0) {
                zero++;
            } else if (count <= 3) {
                oneToThree++;
            } else {
                fourPlus++;
            }
        }
        return List.of(
            new LabelCount("subs_bucket_zero", zero),
            new LabelCount("subs_bucket_one_to_three", oneToThree),
            new LabelCount("subs_bucket_four_plus", fourPlus)
        );
    }

    private List<LabelCount> blockedUsersSlice(List<User> users) {
        long blocked = users.stream().filter(User::isCurrentlyBlocked).count();
        return List.of(
            new LabelCount("user_active", users.size() - blocked),
            new LabelCount("user_blocked", blocked)
        );
    }

    private List<LabelCount> notificationChannelsSlice(long email, long telegram, long push) {
        return List.of(
            new LabelCount("notify_email", email),
            new LabelCount("notify_telegram", telegram),
            new LabelCount("notify_push", push)
        );
    }

    private List<LabelCount> reminderRepeatSlice(List<Reminder> reminders) {
        long once = reminders.stream().filter(reminder -> reminder.getRepeatType() == ReminderRepeat.ONCE).count();
        long monthly = reminders.stream().filter(reminder -> reminder.getRepeatType() == ReminderRepeat.MONTHLY).count();
        long yearly = reminders.stream().filter(reminder -> reminder.getRepeatType() == ReminderRepeat.YEARLY).count();
        return List.of(
            new LabelCount("reminder_once", once),
            new LabelCount("reminder_monthly", monthly),
            new LabelCount("reminder_yearly", yearly)
        );
    }

    private Map<LocalDate, Long> emptyLast7Days() {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            counts.put(LocalDate.now(ZoneOffset.UTC).minusDays(i), 0L);
        }
        return counts;
    }

    private void incrementDay(Map<LocalDate, Long> counts, Instant instant) {
        LocalDate day = instant.atZone(ZoneOffset.UTC).toLocalDate();
        counts.merge(day, 1L, Long::sum);
    }

    private List<DailyCount> toDailyCounts(Map<LocalDate, Long> counts) {
        List<DailyCount> result = new ArrayList<>();
        counts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> result.add(new DailyCount(entry.getKey().toString(), entry.getValue())));
        return result;
    }
}
