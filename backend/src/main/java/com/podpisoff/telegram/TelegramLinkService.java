package com.podpisoff.telegram;

import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.settings.TelegramProperties;
import com.podpisoff.user.User;
import com.podpisoff.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramLinkService {

    private static final int TOKEN_TTL_MINUTES = 15;

    private final AuthFacade authFacade;
    private final UserRepository userRepository;
    private final TelegramLinkTokenRepository linkTokenRepository;
    private final TelegramProperties telegramProperties;

    public TelegramLinkService(AuthFacade authFacade,
                               UserRepository userRepository,
                               TelegramLinkTokenRepository linkTokenRepository,
                               TelegramProperties telegramProperties) {
        this.authFacade = authFacade;
        this.userRepository = userRepository;
        this.linkTokenRepository = linkTokenRepository;
        this.telegramProperties = telegramProperties;
    }

    public boolean isBotConfigured() {
        return telegramProperties.isConfigured() && hasBotUsername();
    }

    @Transactional
    public TelegramLinkResponse createLink() {
        requireBotConfigured();
        User user = authFacade.getCurrentUser();

        TelegramLinkToken linkToken = new TelegramLinkToken();
        linkToken.setUser(user);
        linkToken.setToken(generateToken());
        linkToken.setExpiresAt(Instant.now().plus(TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));
        linkTokenRepository.save(linkToken);

        return new TelegramLinkResponse(
            telegramProperties.botUsername(),
            buildDeepLink(linkToken.getToken()),
            linkToken.getExpiresAt()
        );
    }

    @Transactional(readOnly = true)
    public TelegramLinkStatusResponse linkStatus() {
        if (!isBotConfigured()) {
            return new TelegramLinkStatusResponse(false, false, null, null);
        }
        User user = authFacade.getCurrentUser();
        Instant now = Instant.now();
        boolean linked = hasLinkedChat(user);
        if (linked) {
            return new TelegramLinkStatusResponse(true, false, telegramProperties.botUsername(), null);
        }

        Optional<TelegramLinkToken> pending = linkTokenRepository
            .findFirstByUser_IdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(user.getId(), now);
        if (pending.isEmpty()) {
            return new TelegramLinkStatusResponse(false, false, telegramProperties.botUsername(), null);
        }
        return new TelegramLinkStatusResponse(
            false,
            true,
            telegramProperties.botUsername(),
            buildDeepLink(pending.get().getToken())
        );
    }

    @Transactional
    public void disconnect() {
        User user = authFacade.getCurrentUser();
        user.setTelegramChatId(null);
        user.setTelegramNotificationsEnabled(false);
        user.setTelegramMenuMessageId(null);
        userRepository.save(user);
    }

    @Transactional
    public Optional<User> completeLink(String token, String chatId) {
        requireBotConfigured();
        if (token == null || token.isBlank() || chatId == null || chatId.isBlank()) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        TelegramLinkToken linkToken = linkTokenRepository.findByToken(token.trim())
            .orElse(null);
        if (linkToken == null || linkToken.isUsed() || linkToken.isExpired(now)) {
            return userRepository.findByTelegramChatId(chatId.trim())
                .filter(TelegramLinkService::hasLinkedChat);
        }

        User user = linkToken.getUser();
        user.setTelegramChatId(chatId.trim());
        user.setTelegramNotificationsEnabled(true);
        userRepository.save(user);

        linkToken.setUsedAt(now);
        linkTokenRepository.save(linkToken);

        return Optional.of(user);
    }

    @Transactional
    public void handleStop(String chatId) {
        if (chatId == null || chatId.isBlank()) {
            return;
        }
        userRepository.findByTelegramChatId(chatId.trim()).ifPresent(user -> {
            user.setTelegramChatId(null);
            user.setTelegramNotificationsEnabled(false);
            user.setTelegramMenuMessageId(null);
            userRepository.save(user);
        });
    }

    private static boolean hasLinkedChat(User user) {
        return user.getTelegramChatId() != null && !user.getTelegramChatId().isBlank();
    }

    private String buildDeepLink(String token) {
        return "https://t.me/" + telegramProperties.botUsername() + "?start=" + token;
    }

    private static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void requireBotConfigured() {
        if (!isBotConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Telegram bot is not configured");
        }
    }

    private boolean hasBotUsername() {
        return telegramProperties.botUsername() != null && !telegramProperties.botUsername().isBlank();
    }
}
