package com.podpisoff.auth.oauth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOAuthIdentityRepository extends JpaRepository<UserOAuthIdentity, Long> {
    Optional<UserOAuthIdentity> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
}
