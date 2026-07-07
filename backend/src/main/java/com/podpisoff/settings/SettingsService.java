package com.podpisoff.settings;

import com.podpisoff.auth.AuthResponse;
import com.podpisoff.auth.AuthService;
import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.notification.EmailNotificationService;
import com.podpisoff.notification.TelegramNotificationService;
import com.podpisoff.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private final AuthFacade authFacade;
    private final AuthService authService;
    private final EmailNotificationService emailNotificationService;
    private final TelegramNotificationService telegramNotificationService;
    private final TelegramProperties telegramProperties;

    public SettingsService(AuthFacade authFacade,
                           AuthService authService,
                           EmailNotificationService emailNotificationService,
                           TelegramNotificationService telegramNotificationService,
                           TelegramProperties telegramProperties) {
        this.authFacade = authFacade;
        this.authService = authService;
        this.emailNotificationService = emailNotificationService;
        this.telegramNotificationService = telegramNotificationService;
        this.telegramProperties = telegramProperties;
    }

    public NotificationChannelsResponse notificationChannels() {
        return new NotificationChannelsResponse(
            emailNotificationService.isConfigured(),
            telegramNotificationService.isConfigured(),
            telegramProperties.botUsername()
        );
    }

    @Transactional
    public AuthResponse update(SettingsUpdateRequest request) {
        if (request.billingReminderDaysBefore() == null
            && request.emailNotificationsEnabled() == null
            && request.telegramNotificationsEnabled() == null
            && request.telegramChatId() == null
            && request.email() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No settings provided");
        }

        User user = authFacade.getCurrentUser();
        if (request.billingReminderDaysBefore() != null) {
            try {
                user.setBillingReminderDaysBefore(BillingReminderDays.normalize(request.billingReminderDaysBefore()));
            } catch (IllegalArgumentException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid billing reminder days");
            }
        }
        if (request.email() != null) {
            String nextEmail = request.email().trim();
            if (!nextEmail.isBlank() && !nextEmail.contains("@")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid email");
            }
            user.setEmail(nextEmail.isBlank() ? null : nextEmail);
        }
        if (request.emailNotificationsEnabled() != null) {
            user.setEmailNotificationsEnabled(request.emailNotificationsEnabled());
        }
        if (request.telegramNotificationsEnabled() != null) {
            user.setTelegramNotificationsEnabled(request.telegramNotificationsEnabled());
        }
        if (request.telegramChatId() != null) {
            String chatId = request.telegramChatId().trim();
            user.setTelegramChatId(chatId.isBlank() ? null : chatId);
        }

        if (user.isEmailNotificationsEnabled() && (user.getEmail() == null || user.getEmail().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required for email notifications");
        }
        if (user.isTelegramNotificationsEnabled() && (user.getTelegramChatId() == null || user.getTelegramChatId().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Telegram chat id is required for Telegram notifications");
        }

        return authService.toAuthResponse(user);
    }

    public AuthResponse currentUser() {
        return authService.toAuthResponse(authFacade.getCurrentUser());
    }
}
