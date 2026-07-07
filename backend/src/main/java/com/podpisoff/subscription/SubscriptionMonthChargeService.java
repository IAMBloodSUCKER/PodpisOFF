package com.podpisoff.subscription;

import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.user.User;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionMonthChargeService {

    private final SubscriptionMonthChargeRepository chargeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AuthFacade authFacade;

    public SubscriptionMonthChargeService(SubscriptionMonthChargeRepository chargeRepository,
                                            SubscriptionRepository subscriptionRepository,
                                            AuthFacade authFacade) {
        this.chargeRepository = chargeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.authFacade = authFacade;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionMonthChargeResponse> listForSubscription(Long subscriptionId) {
        Subscription subscription = requireOwnedSubscription(subscriptionId);
        return chargeRepository.findAllBySubscriptionIdOrderByChargeYearDescChargeMonthDesc(subscription.getId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public SubscriptionMonthChargeResponse upsert(Long subscriptionId, SubscriptionMonthChargeRequest request) {
        Subscription subscription = requireOwnedSubscription(subscriptionId);
        validateMonth(request.year(), request.month());

        SubscriptionMonthCharge charge = chargeRepository
            .findBySubscriptionIdAndChargeYearAndChargeMonth(subscription.getId(), request.year(), request.month())
            .orElseGet(SubscriptionMonthCharge::new);

        charge.setSubscription(subscription);
        charge.setChargeYear(request.year());
        charge.setChargeMonth(request.month());
        charge.setAmount(request.amount());
        charge.setNote(SubscriptionExtras.blankToNull(request.note()));

        return toResponse(chargeRepository.save(charge));
    }

    @Transactional
    public void delete(Long subscriptionId, int year, int month) {
        Subscription subscription = requireOwnedSubscription(subscriptionId);
        validateMonth(year, month);
        chargeRepository.deleteBySubscriptionIdAndChargeYearAndChargeMonth(subscription.getId(), year, month);
    }

    private Subscription requireOwnedSubscription(Long subscriptionId) {
        User user = authFacade.getCurrentUser();
        return subscriptionRepository.findByIdAndUserId(subscriptionId, user.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subscription not found"));
    }

    private void validateMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid month");
        }
        if (year < 2000 || year > 2100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid year");
        }
    }

    private SubscriptionMonthChargeResponse toResponse(SubscriptionMonthCharge charge) {
        return new SubscriptionMonthChargeResponse(
            charge.getId(),
            charge.getSubscription().getId(),
            charge.getChargeYear(),
            charge.getChargeMonth(),
            charge.getAmount(),
            charge.getNote()
        );
    }
}
