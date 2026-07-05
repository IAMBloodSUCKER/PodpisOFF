package com.podpisoff.dashboard;

import com.podpisoff.common.AuthFacade;
import com.podpisoff.subscription.Subscription;
import com.podpisoff.subscription.SubscriptionRepository;
import com.podpisoff.subscription.SubscriptionResponse;
import com.podpisoff.subscription.SubscriptionService;
import com.podpisoff.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final AuthFacade authFacade;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    public DashboardService(AuthFacade authFacade,
                            SubscriptionRepository subscriptionRepository,
                            SubscriptionService subscriptionService) {
        this.authFacade = authFacade;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
    }

    public DashboardSummaryResponse summary(Integer year, Integer month) {
        User user = authFacade.getCurrentUser();
        LocalDate now = LocalDate.now();
        int targetYear = year == null ? now.getYear() : year;
        int targetMonth = month == null ? now.getMonthValue() : month;
        YearMonth ym = YearMonth.of(targetYear, targetMonth);

        List<Subscription> all = subscriptionRepository.findAllByUserIdOrderByNextBillingDateAsc(user.getId());
        List<Subscription> active = all.stream().filter(Subscription::isActive).toList();

        BigDecimal monthlyTotal = active.stream()
            .filter(s -> YearMonth.from(s.getNextBillingDate()).equals(ym))
            .map(Subscription::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal yearlyTotal = active.stream()
            .filter(s -> s.getNextBillingDate().getYear() == targetYear)
            .map(Subscription::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        active.stream()
            .filter(s -> YearMonth.from(s.getNextBillingDate()).equals(ym))
            .forEach(s -> byCategory.merge(s.getCategory(), s.getAmount(), BigDecimal::add));

        List<SubscriptionResponse> upcoming = subscriptionRepository
            .findTop10ByUserIdAndActiveIsTrueAndNextBillingDateBetweenOrderByNextBillingDateAsc(
                user.getId(), now, now.plusDays(31)
            )
            .stream()
            .map(subscriptionService::toResponse)
            .toList();

        return new DashboardSummaryResponse(monthlyTotal, yearlyTotal, byCategory, upcoming);
    }
}
