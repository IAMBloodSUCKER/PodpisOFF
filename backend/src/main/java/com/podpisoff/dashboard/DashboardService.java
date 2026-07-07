package com.podpisoff.dashboard;

import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.subscription.BillingPeriodMath;
import com.podpisoff.subscription.Subscription;
import com.podpisoff.subscription.SubscriptionMonthCharge;
import com.podpisoff.subscription.SubscriptionMonthChargeRepository;
import com.podpisoff.subscription.SubscriptionRepository;
import com.podpisoff.subscription.SubscriptionResponse;
import com.podpisoff.subscription.SubscriptionService;
import com.podpisoff.user.PlanAccessService;
import com.podpisoff.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final AuthFacade authFacade;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMonthChargeRepository monthChargeRepository;
    private final SubscriptionService subscriptionService;
    private final PlanAccessService planAccessService;

    public DashboardService(AuthFacade authFacade,
                            SubscriptionRepository subscriptionRepository,
                            SubscriptionMonthChargeRepository monthChargeRepository,
                            SubscriptionService subscriptionService,
                            PlanAccessService planAccessService) {
        this.authFacade = authFacade;
        this.subscriptionRepository = subscriptionRepository;
        this.monthChargeRepository = monthChargeRepository;
        this.subscriptionService = subscriptionService;
        this.planAccessService = planAccessService;
    }

    public DashboardSummaryResponse summary(Integer year, Integer month) {
        User user = authFacade.getCurrentUser();
        LocalDate now = LocalDate.now();
        int targetYear = year == null ? now.getYear() : year;
        int targetMonth = month == null ? now.getMonthValue() : month;
        if (targetMonth < 1 || targetMonth > 12) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid month");
        }
        if (targetYear < 2000 || targetYear > 2100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid year");
        }

        subscriptionService.rolloverDueDates(user);
        List<Subscription> all = subscriptionRepository.findAllByUserIdOrderByNextBillingDateAsc(user.getId());
        Set<Long> includedIds = planAccessService.includedSubscriptionIds(user, all);
        List<Subscription> active = planAccessService.filterIncludedSubscriptions(
            user,
            all.stream().filter(Subscription::isActive).toList(),
            includedIds
        );

        Map<String, BigDecimal> monthlyByCurrency = new LinkedHashMap<>();
        active.forEach(subscription -> monthlyByCurrency.merge(
            subscription.getCurrency(),
            BillingPeriodMath.monthlyBurn(subscription.getAmount(), subscription.getBillingPeriod()),
            BigDecimal::add
        ));

        Map<String, BigDecimal> yearlyByCurrency = new LinkedHashMap<>();
        active.forEach(subscription -> yearlyByCurrency.merge(
            subscription.getCurrency(),
            BillingPeriodMath.yearlyBurn(subscription.getAmount(), subscription.getBillingPeriod()),
            BigDecimal::add
        ));

        Map<Long, BigDecimal> overrides = loadOverrides(user.getId(), targetYear, targetMonth);
        Map<String, BigDecimal> monthSpendByCurrency = SubscriptionSpendingCalculator.monthSpendByCurrency(
            active, overrides, targetYear, targetMonth
        );

        Map<String, Map<String, BigDecimal>> byCategory = SubscriptionSpendingCalculator.byCategoryMonthlyBurn(active);

        List<SubscriptionResponse> upcoming = subscriptionRepository
            .findTop10ByUserIdAndActiveIsTrueAndNextBillingDateBetweenOrderByNextBillingDateAsc(
                user.getId(), now, now.plusDays(31)
            )
            .stream()
            .filter(subscription -> includedIds.contains(subscription.getId()))
            .map(subscriptionService::toResponse)
            .toList();

        return new DashboardSummaryResponse(
            targetYear,
            targetMonth,
            monthlyByCurrency,
            yearlyByCurrency,
            monthSpendByCurrency,
            byCategory,
            upcoming
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
