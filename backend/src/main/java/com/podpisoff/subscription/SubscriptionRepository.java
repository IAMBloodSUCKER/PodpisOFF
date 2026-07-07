package com.podpisoff.subscription;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findAllByUserIdOrderByNextBillingDateAsc(Long userId);

    Optional<Subscription> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    long countByActiveTrue();

    List<Subscription> findTop10ByUserIdAndActiveIsTrueAndNextBillingDateBetweenOrderByNextBillingDateAsc(
        Long userId, LocalDate start, LocalDate end
    );
}
