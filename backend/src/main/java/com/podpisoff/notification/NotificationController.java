package com.podpisoff.notification;

import com.podpisoff.common.AuthFacade;
import com.podpisoff.user.User;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthFacade authFacade;

    public NotificationController(NotificationService notificationService, AuthFacade authFacade) {
        this.notificationService = notificationService;
        this.authFacade = authFacade;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        User user = authFacade.getCurrentUser();
        return notificationService.listForUser(user).stream()
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount() {
        User user = authFacade.getCurrentUser();
        return new UnreadCountResponse(notificationService.unreadCount(user));
    }

    @PatchMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        notificationService.markRead(authFacade.getCurrentUser(), id);
    }

    @PatchMapping("/read-all")
    public void markAllRead() {
        notificationService.markAllRead(authFacade.getCurrentUser());
    }

    private NotificationResponse toResponse(UserNotification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getType().name(),
            notification.getTitle(),
            notification.getBody(),
            notification.getReferenceId(),
            notification.getCreatedAt(),
            notification.getReadAt(),
            !notification.isRead()
        );
    }
}
