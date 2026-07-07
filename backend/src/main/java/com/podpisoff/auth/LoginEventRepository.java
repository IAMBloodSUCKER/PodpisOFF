package com.podpisoff.auth;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {

    long countByLoggedInAtAfter(Instant since);

    long countDistinctUser_IdByLoggedInAtAfter(Instant since);

    void deleteByUser_Id(Long userId);
}
