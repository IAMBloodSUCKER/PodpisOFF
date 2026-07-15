package com.podpisoff.notification;

import com.podpisoff.user.User;
import org.springframework.stereotype.Service;

@Service
public class NotificationChannelService {

    private final EmailNotificationService emailNotificationService;
    private final TelegramNotificationService telegramNotificationService;

    public NotificationChannelService(EmailNotificationService emailNotificationService,
                                        TelegramNotificationService telegramNotificationService) {
        this.emailNotificationService = emailNotificationService;
        this.telegramNotificationService = telegramNotificationService;
    }

    public void deliver(User user, String title, String body) {
        if (user == null || title == null || title.isBlank()) {
            return;
        }
        String message = body == null || body.isBlank() ? title : title + "\n\n" + body;
        if (user.isEmailNotificationsEnabled()) {
            emailNotificationService.send(user.getEmail(), title, body);
        }
        if (user.isTelegramNotificationsEnabled()) {
            telegramNotificationService.sendPush(user.getTelegramChatId(), title, body);
        }
    }
}
