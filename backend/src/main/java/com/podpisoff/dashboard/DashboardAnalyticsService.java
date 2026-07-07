package com.podpisoff.dashboard;

import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.subscription.Subscription;
import com.podpisoff.subscription.SubscriptionMonthCharge;
import com.podpisoff.subscription.SubscriptionMonthChargeRepository;
import com.podpisoff.subscription.SubscriptionRepository;
import com.podpisoff.subscription.SubscriptionService;
import com.podpisoff.user.Plan;
import com.podpisoff.user.PlanAccessService;
import com.podpisoff.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DashboardAnalyticsService {

    private static final int MAX_MONTHS = 12;

    private final AuthFacade authFacade;
    private final PlanAccessService planAccessService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMonthChargeRepository monthChargeRepository;
    private final SubscriptionService subscriptionService;

    public DashboardAnalyticsService(AuthFacade authFacade,
                                     PlanAccessService planAccessService,
                                     SubscriptionRepository subscriptionRepository,
                                     SubscriptionMonthChargeRepository monthChargeRepository,
                                     SubscriptionService subscriptionService) {
        this.authFacade = authFacade;
        this.planAccessService = planAccessService;
        this.subscriptionRepository = subscriptionRepository;
        this.monthChargeRepository = monthChargeRepository;
        this.subscriptionService = subscriptionService;
    }

    public DashboardAnalyticsResponse analytics(Integer months) {
        User user = authFacade.getCurrentUser();
        if (planAccessService.effectivePlan(user) != Plan.PRO) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Analytics available only for PRO plan");
        }

        int monthCount = months == null ? 6 : months;
        if (monthCount < 3 || monthCount > MAX_MONTHS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid months");
        }

        subscriptionService.rolloverDueDates(user);
        List<Subscription> all = subscriptionRepository.findAllByUserIdOrderByNextBillingDateAsc(user.getId());
        List<Subscription> active = all.stream()
            .filter(Subscription::isActive)
            .toList();
        LocalDate today = LocalDate.now();

        List<DashboardAnalyticsResponse.MonthSpendPoint> trend = new ArrayList<>();
        YearMonth cursor = YearMonth.now().minusMonths(monthCount - 1L);
        YearMonth end = YearMonth.now();
        while (!cursor.isAfter(end)) {
            Map<Long, BigDecimal> overrides = loadOverrides(user.getId(), cursor.getYear(), cursor.getMonthValue());
            Map<String, BigDecimal> spend = SubscriptionSpendingCalculator.actualMonthSpendByCurrency(
                all,
                overrides,
                cursor.getYear(),
                cursor.getMonthValue(),
                today
            );
            trend.add(new DashboardAnalyticsResponse.MonthSpendPoint(cursor.getYear(), cursor.getMonthValue(), spend));
            cursor = cursor.plusMonths(1);
        }

        List<DashboardAnalyticsResponse.TopSubscriptionItem> top = active.stream()
            .sorted(Comparator.comparing(SubscriptionSpendingCalculator::monthlyBurn).reversed())
            .limit(8)
            .map(subscription -> new DashboardAnalyticsResponse.TopSubscriptionItem(
                subscription.getId(),
                subscription.getTitle(),
                subscription.getCategory(),
                SubscriptionSpendingCalculator.monthlyBurn(subscription),
                subscription.getCurrency(),
                subscription.getBillingPeriod()
            ))
            .toList();

        return new DashboardAnalyticsResponse(
            trend,
            top,
            SubscriptionSpendingCalculator.byCategoryMonthlyBurn(active)
        );
    }

    private Map<Long, BigDecimal> loadOverrides(Long userId, int year, int month) {
        Map<Long, BigDecimal> overrides = new HashMap<>();
        for (SubscriptionMonthCharge charge : monthChargeRepository.findAllBySubscription_User_IdAndChargeYearAndChargeMonth(
            userId, year, month
        )) {
            overrides.put(charge.getSubscription().getId(), charge.getAmount());
        }
        return overrides;
    }
}
