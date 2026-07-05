package com.podpisoff.subscription;

import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.user.Plan;
import com.podpisoff.user.User;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

    private static final int FREE_PLAN_LIMIT = 5;

    private final SubscriptionRepository subscriptionRepository;
    private final AuthFacade authFacade;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, AuthFacade authFacade) {
        this.subscriptionRepository = subscriptionRepository;
        this.authFacade = authFacade;
    }

    public List<SubscriptionResponse> list() {
        User user = authFacade.getCurrentUser();
        return subscriptionRepository.findAllByUserIdOrderByNextBillingDateAsc(user.getId()).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public SubscriptionResponse create(SubscriptionRequest request) {
        User user = authFacade.getCurrentUser();
        long currentCount = subscriptionRepository.countByUserId(user.getId());
        if (user.getPlan() == Plan.FREE && currentCount >= FREE_PLAN_LIMIT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FREE plan allows up to 5 subscriptions");
        }
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
        subscription.setTitle(request.title().trim());
        subscription.setCategory(request.category().trim());
        subscription.setAmount(request.amount());
        subscription.setCurrency(request.currency().trim().toUpperCase());
        subscription.setNextBillingDate(request.nextBillingDate());
        subscription.setActive(request.active());
    }

    public SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
            subscription.getId(),
            subscription.getTitle(),
            subscription.getCategory(),
            subscription.getAmount(),
            subscription.getCurrency(),
            subscription.getNextBillingDate(),
            subscription.isActive(),
            subscription.getCreatedAt(),
            subscription.getUpdatedAt()
        );
    }
}
