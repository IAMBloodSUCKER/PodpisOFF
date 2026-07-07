package com.podpisoff.subscription;

import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.common.SupportedCurrencies;
import com.podpisoff.user.Plan;
import com.podpisoff.user.PlanAccessService;
import com.podpisoff.user.PlanLimits;
import com.podpisoff.user.User;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

    private static final int FREE_PLAN_LIMIT = PlanLimits.FREE_SUBSCRIPTION_LIMIT;

    private final SubscriptionRepository subscriptionRepository;
    private final AuthFacade authFacade;
    private final PlanAccessService planAccessService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               AuthFacade authFacade,
                               PlanAccessService planAccessService) {
        this.subscriptionRepository = subscriptionRepository;
        this.authFacade = authFacade;
        this.planAccessService = planAccessService;
    }

    @Transactional
    public List<SubscriptionResponse> list() {
        User user = authFacade.getCurrentUser();
        return rolloverAll(user).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public void rolloverDueDates(User user) {
        rolloverAll(user);
    }

    private List<Subscription> rolloverAll(User user) {
        List<Subscription> subscriptions = subscriptionRepository.findAllByUserIdOrderByNextBillingDateAsc(user.getId());
        LocalDate today = LocalDate.now();
        for (Subscription subscription : subscriptions) {
            LocalDate effective = SubscriptionBillingSchedule.effectiveNextDate(
                subscription.getBillingPeriod(),
                subscription.getNextBillingDate(),
                today
            );
            if (!effective.equals(subscription.getNextBillingDate())) {
                subscription.setNextBillingDate(effective);
                subscriptionRepository.save(subscription);
            }
        }
        return subscriptions;
    }

    @Transactional
    public SubscriptionResponse create(SubscriptionRequest request) {
        User user = authFacade.getCurrentUser();
        long currentCount = subscriptionRepository.countByUserId(user.getId());
        if (planAccessService.effectivePlan(user) == Plan.FREE && currentCount >= FREE_PLAN_LIMIT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FREE plan allows up to 3 subscriptions");
        }
        planAccessService.validateSubscriptionCurrency(user, request.currency(), null);
        Subscription subscription = new Subscription();
        applyRequest(subscription, request);
        subscription.setUser(user);
        return toResponse(subscriptionRepository.save(subscription));
    }

    @Transactional
    public SubscriptionResponse update(Long id, SubscriptionRequest request) {
        User user = authFacade.getCurrentUser();
        Subscription subscription = subscriptionRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subscription not found"));
        planAccessService.validateSubscriptionCurrency(user, request.currency(), subscription.getCurrency());
        applyRequest(subscription, request);
        return toResponse(subscription);
    }

    @Transactional
    public void delete(Long id) {
        User user = authFacade.getCurrentUser();
        Subscription subscription = subscriptionRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subscription not found"));
        subscriptionRepository.delete(subscription);
    }

    private void applyRequest(Subscription subscription, SubscriptionRequest request) {
        String currency = SupportedCurrencies.normalize(request.currency());
        if (!SupportedCurrencies.isSupported(currency)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported currency");
        }
        subscription.setTitle(request.title().trim());
        subscription.setCategory(request.category().trim());
        subscription.setAmount(request.amount());
        subscription.setCurrency(currency);
        subscription.setNextBillingDate(request.nextBillingDate());
        subscription.setBillingPeriod(request.billingPeriod() == null ? BillingPeriod.MONTHLY : request.billingPeriod());
        subscription.setActive(request.active());
        subscription.setNote(SubscriptionExtras.blankToNull(request.note()));
        subscription.setResourceUrl(SubscriptionExtras.normalizeResourceUrl(request.resourceUrl()));
    }

    public SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
            subscription.getId(),
            subscription.getTitle(),
            subscription.getCategory(),
            subscription.getAmount(),
            subscription.getCurrency(),
            subscription.getNextBillingDate(),
            subscription.getBillingPeriod(),
            subscription.isActive(),
            subscription.getNote(),
            subscription.getResourceUrl(),
            subscription.getCreatedAt(),
            subscription.getUpdatedAt()
        );
    }
}
