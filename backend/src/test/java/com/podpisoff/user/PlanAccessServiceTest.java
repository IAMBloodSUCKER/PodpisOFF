package com.podpisoff.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.podpisoff.subscription.Subscription;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlanAccessServiceTest {

    private final PlanAccessService service = new PlanAccessService();

    @Test
    void effectivePlanTreatsActiveProTrialAsPro() {
        User user = new User();
        user.setPlan(Plan.PRO);
        user.setPlanExpiresAt(LocalDateTime.now().plusDays(PlanAccessService.PRO_TRIAL_DAYS));

        assertEquals(Plan.PRO, service.effectivePlan(user));
    }

    @Test
    void effectivePlanTreatsExpiredProAsFree() {
        User user = new User();
        user.setPlan(Plan.PRO);
        user.setPlanExpiresAt(LocalDateTime.now().minusDays(1));

        assertEquals(Plan.FREE, service.effectivePlan(user));
    }

    @Test
    void includedSubscriptionIdsLimitsFreePlanToOldestThree() {
        User user = new User();
        user.setPlan(Plan.FREE);

        List<Subscription> subscriptions = List.of(
            subscription(10L, Instant.parse("2026-04-01T00:00:00Z")),
            subscription(11L, Instant.parse("2026-02-01T00:00:00Z")),
            subscription(12L, Instant.parse("2026-01-01T00:00:00Z")),
            subscription(13L, Instant.parse("2026-03-01T00:00:00Z"))
        );

        Set<Long> included = service.includedSubscriptionIds(user, subscriptions);

        assertEquals(Set.of(12L, 11L, 13L), included);
        assertFalse(included.contains(10L));
    }

    @Test
    void expiredProUsesFreeSubscriptionLimit() {
        User user = new User();
        user.setPlan(Plan.PRO);
        user.setPlanExpiresAt(LocalDateTime.now().minusDays(1));

        List<Subscription> subscriptions = List.of(
            subscription(1L, Instant.parse("2026-01-01T00:00:00Z")),
            subscription(2L, Instant.parse("2026-02-01T00:00:00Z")),
            subscription(3L, Instant.parse("2026-03-01T00:00:00Z")),
            subscription(4L, Instant.parse("2026-04-01T00:00:00Z"))
        );

        Set<Long> included = service.includedSubscriptionIds(user, subscriptions);
        List<Subscription> forSpending = service.filterIncludedSubscriptions(user, subscriptions, included);

        assertEquals(3, forSpending.size());
        assertTrue(forSpending.stream().map(Subscription::getId).noneMatch(id -> id == 4L));
    }

    @Test
    void includedSubscriptionIdsSkipsPausedOnFreePlan() {
        User user = new User();
        user.setPlan(Plan.FREE);

        List<Subscription> subscriptions = List.of(
            subscription(1L, Instant.parse("2026-01-01T00:00:00Z"), true),
            subscription(2L, Instant.parse("2026-02-01T00:00:00Z"), false),
            subscription(3L, Instant.parse("2026-03-01T00:00:00Z"), true),
            subscription(4L, Instant.parse("2026-04-01T00:00:00Z"), true)
        );

        Set<Long> included = service.includedSubscriptionIds(user, subscriptions);

        assertEquals(Set.of(1L, 3L, 4L), included);
    }

    private static Subscription subscription(Long id, Instant createdAt) {
        return subscription(id, createdAt, true);
    }

    private static Subscription subscription(Long id, Instant createdAt, boolean active) {
        Subscription subscription = mock(Subscription.class);
        when(subscription.getId()).thenReturn(id);
        when(subscription.getCreatedAt()).thenReturn(createdAt);
        when(subscription.isActive()).thenReturn(active);
        return subscription;
    }
}
