package com.podpisoff.subscription;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionMonthChargeRepository extends JpaRepository<SubscriptionMonthCharge, Long> {

    List<SubscriptionMonthCharge> findAllBySubscriptionIdOrderByChargeYearDescChargeMonthDesc(Long subscriptionId);

    Optional<SubscriptionMonthCharge> findBySubscriptionIdAndChargeYearAndChargeMonth(
        Long subscriptionId,
        int chargeYear,
        int chargeMonth
    );

    List<SubscriptionMonthCharge> findAllBySubscription_User_IdAndChargeYearAndChargeMonth(
        Long userId,
        int chargeYear,
        int chargeMonth
    );

    void deleteBySubscriptionIdAndChargeYearAndChargeMonth(Long subscriptionId, int chargeYear, int chargeMonth);
}
