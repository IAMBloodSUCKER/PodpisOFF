package com.podpisoff.auth.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.podpisoff.auth.AuthResponse;
import com.podpisoff.auth.AuthService;
import com.podpisoff.auth.LoginEventService;
import com.podpisoff.auth.oauth.OAuthFlowStore.StateEntry;
import com.podpisoff.auth.oauth.YandexOAuthClient.YandexUserInfo;
import com.podpisoff.common.ApiException;
import com.podpisoff.user.LocaleCode;
import com.podpisoff.user.Plan;
import com.podpisoff.user.PlanAccessService;
import com.podpisoff.user.User;
import com.podpisoff.user.UserRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class YandexOAuthService {

    private final YandexOAuthClient yandexOAuthClient;
    private final YandexOAuthProperties properties;
    private final OAuthFlowStore flowStore;
    private final UserOAuthIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final LoginEventService loginEventService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final String siteUrl;

    public YandexOAuthService(YandexOAuthClient yandexOAuthClient,
                              YandexOAuthProperties properties,
                              OAuthFlowStore flowStore,
                              UserOAuthIdentityRepository identityRepository,
                              UserRepository userRepository,
                              AuthService authService,
                              LoginEventService loginEventService,
                              PasswordEncoder passwordEncoder,
                              ObjectMapper objectMapper,
                              @Value("${app.site-url:http://localhost:3000}") String siteUrl) {
        this.yandexOAuthClient = yandexOAuthClient;
        this.properties = properties;
        this.flowStore = flowStore;
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.loginEventService = loginEventService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.siteUrl = siteUrl == null || siteUrl.isBlank() ? "http://localhost:3000" : siteUrl.trim();
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public String startAuthorizeUrl(LocaleCode locale, String timezone, boolean termsAccepted) {
        String state = flowStore.createState(locale, normalizeTimezone(timezone), termsAccepted);
        return yandexOAuthClient.buildAuthorizeUrl(state);
    }

    @Transactional
    public String handleCallback(String code, String state, String error) {
        if (error != null && !error.isBlank()) {
            return frontendErrorRedirect("oauth_denied");
        }
        if (code == null || code.isBlank()) {
            return frontendErrorRedirect("oauth_missing_code");
        }

        StateEntry stateEntry = flowStore.consumeState(state);
        String accessToken = yandexOAuthClient.exchangeCode(code);
        YandexUserInfo profile = yandexOAuthClient.fetchUserInfo(accessToken);
        User user = findOrCreateUser(profile, stateEntry);
        if (user.isCurrentlyBlocked()) {
            return frontendErrorRedirect("oauth_blocked");
        }
        loginEventService.recordLogin(user);
        AuthResponse auth = authService.toAuthResponse(user);
        try {
            String ticket = flowStore.createTicket(objectMapper.writeValueAsString(auth));
            return UriComponentsBuilder
                .fromUriString(trimSlash(siteUrl) + "/auth/oauth/callback")
                .queryParam("ticket", ticket)
                .build()
                .toUriString();
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create OAuth ticket");
        }
    }

    public AuthResponse exchangeTicket(String ticket) {
        String json = flowStore.consumeTicket(ticket);
        try {
            return objectMapper.readValue(json, AuthResponse.class);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid OAuth ticket");
        }
    }

    public String frontendErrorRedirect(String reason) {
        return UriComponentsBuilder
            .fromUriString(trimSlash(siteUrl) + "/auth")
            .queryParam("oauthError", reason)
            .build()
            .toUriString();
    }

    private User findOrCreateUser(YandexUserInfo profile, StateEntry state) {
        Optional<UserOAuthIdentity> existing = identityRepository
            .findByProviderAndProviderUserId(OAuthProvider.YANDEX, profile.id());
        if (existing.isPresent()) {
            User user = existing.get().getUser();
            if (profile.email() != null && (user.getEmail() == null || user.getEmail().isBlank())) {
                user.setEmail(profile.email());
            }
            return user;
        }

        User user = null;
        if (profile.email() != null && !profile.email().isBlank()) {
            user = userRepository.findFirstByEmailIgnoreCase(profile.email()).orElse(null);
        }
        if (user == null) {
            if (!state.termsAccepted()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Terms must be accepted");
            }
            user = createUser(profile, state);
        } else if (profile.email() != null && (user.getEmail() == null || user.getEmail().isBlank())) {
            user.setEmail(profile.email());
        }

        UserOAuthIdentity identity = new UserOAuthIdentity();
        identity.setUser(user);
        identity.setProvider(OAuthProvider.YANDEX);
        identity.setProviderUserId(profile.id());
        identity.setEmail(profile.email());
        identityRepository.save(identity);
        return user;
    }

    private User createUser(YandexUserInfo profile, StateEntry state) {
        User user = new User();
        user.setUsername(allocateUsername(profile));
        String randomSecret = UUID.randomUUID().toString().replace("-", "");
        user.setPasswordHash(passwordEncoder.encode(randomSecret));
        user.setRecoveryKeyHash(passwordEncoder.encode(UUID.randomUUID().toString().replace("-", "").substring(0, 12)));
        user.setEmail(profile.email());
        user.setPlan(Plan.PRO);
        user.setPlanExpiresAt(LocalDateTime.now().plusDays(PlanAccessService.PRO_TRIAL_DAYS));
        user.setTimezone(state.timezone());
        user.setLocale(state.locale());
        user.setTermsAccepted(true);
        return userRepository.save(user);
    }

    private String allocateUsername(YandexUserInfo profile) {
        String base = sanitizeUsername(profile.login());
        if (base == null) {
            base = sanitizeUsername(profile.displayName());
        }
        if (base == null && profile.email() != null && profile.email().contains("@")) {
            base = sanitizeUsername(profile.email().substring(0, profile.email().indexOf('@')));
        }
        if (base == null) {
            base = "yandex";
        }
        if (base.length() < 3) {
            base = (base + "usr").substring(0, Math.min(3, base.length() + 3));
            while (base.length() < 3) {
                base = base + "x";
            }
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }

        String candidate = base;
        int attempt = 0;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            attempt++;
            String suffix = String.valueOf(attempt);
            int maxBase = Math.max(3, 50 - suffix.length());
            candidate = base.substring(0, Math.min(base.length(), maxBase)) + suffix;
            if (attempt > 9999) {
                candidate = "yandex" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                break;
            }
        }
        return candidate;
    }

    private static String sanitizeUsername(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
        return cleaned.isBlank() ? null : cleaned;
    }

    private static String normalizeTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return "Europe/Moscow";
        }
        String trimmed = timezone.trim();
        return trimmed.length() > 64 ? "Europe/Moscow" : trimmed;
    }

    private static String trimSlash(String url) {
        return url.replaceAll("/$", "");
    }
}
