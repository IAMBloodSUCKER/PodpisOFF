package com.podpisoff.user;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    long countByCreatedAtAfter(Instant since);

    long countByPlan(Plan plan);

    long countByPlanAndPlanExpiresAtBefore(Plan plan, LocalDateTime expiresBefore);

    Optional<User> findByTelegramChatId(String telegramChatId);
}
