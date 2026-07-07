package com.podpisoff.notification;

import com.podpisoff.user.User;
import com.podpisoff.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class NotificationService {

    private final UserNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final com.podpisoff.push.PushServiceFacade pushServiceFacade;
    private final NotificationChannelService notificationChannelService;

    public NotificationService(UserNotificationRepository notificationRepository,
                               UserRepository userRepository,
                               com.podpisoff.push.PushServiceFacade pushServiceFacade,
                               NotificationChannelService notificationChannelService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.pushServiceFacade = pushServiceFacade;
        this.notificationChannelService = notificationChannelService;
    }

    @Transactional
    public UserNotification create(User user, NotificationType type, String title, String body, Long referenceId) {
        UserNotification notification = new UserNotification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setReferenceId(referenceId);
        UserNotification saved = notificationRepository.save(notification);
        scheduleDeliveryAfterCommit(user, title, body, saved.getId());
        return saved;
    }

    private void scheduleDeliveryAfterCommit(User user, String title, String body, Long notificationId) {
        Runnable deliver = () -> {
            pushServiceFacade.sendToUser(
                user.getId(),
                title,
                body,
                "notification-" + notificationId
            );
            notificationChannelService.deliver(user, title, body);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deliver.run();
                }
            });
        } else {
            deliver.run();
        }
    }

    @Transactional
    public UserNotification createForUserId(Long userId, NotificationType type, String title, String body, Long referenceId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new com.podpisoff.common.ApiException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "User not found"
            ));
        return create(user, type, title, body, referenceId);
    }

    public List<UserNotification> listForUser(User user) {
        return notificationRepository.findTop30ByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public long unreadCount(User user) {
        return notificationRepository.countByUserIdAndReadAtIsNull(user.getId());
    }

    @Transactional
    public void markRead(User user, Long notificationId) {
        UserNotification notification = notificationRepository.findById(notificationId)
            .filter(item -> item.getUser().getId().equals(user.getId()))
            .orElseThrow(() -> new com.podpisoff.common.ApiException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Notification not found"
            ));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
    }

    @Transactional
    public void markAllRead(User user) {
        notificationRepository.findTop30ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
            .filter(item -> item.getReadAt() == null)
            .forEach(item -> item.setReadAt(Instant.now()));
    }
}
