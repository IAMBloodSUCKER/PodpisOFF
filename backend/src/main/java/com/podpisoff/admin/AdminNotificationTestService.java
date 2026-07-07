package com.podpisoff.admin;

import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.notification.NotificationService;
import com.podpisoff.notification.NotificationType;
import com.podpisoff.user.User;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AdminNotificationTestService {

    private static final Logger log = LoggerFactory.getLogger(AdminNotificationTestService.class);
    private static final Set<Integer> ALLOWED_DELAYS = Set.of(0, 5, 10);

    private final AdminAccessService adminAccessService;
    private final AuthFacade authFacade;
    private final NotificationService notificationService;
    private final ScheduledExecutorService scheduledExecutorService;

    public AdminNotificationTestService(AdminAccessService adminAccessService,
                                        AuthFacade authFacade,
                                        NotificationService notificationService,
                                        ScheduledExecutorService scheduledExecutorService) {
        this.adminAccessService = adminAccessService;
        this.authFacade = authFacade;
        this.notificationService = notificationService;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public AdminTestNotificationResponse schedule(HttpServletRequest request, int delaySeconds) {
        adminAccessService.requireAdminKey(request);
        if (!ALLOWED_DELAYS.contains(delaySeconds)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "delaySeconds must be 0, 5, or 10");
        }

        User user = authFacade.getCurrentUser();
        Long userId = user.getId();
        Instant deliverAt = Instant.now().plusSeconds(delaySeconds);

        if (delaySeconds == 0) {
            deliver(userId, delaySeconds);
        } else {
            scheduledExecutorService.schedule(
                () -> deliver(userId, delaySeconds),
                delaySeconds,
                TimeUnit.SECONDS
            );
        }

        return new AdminTestNotificationResponse(delaySeconds, deliverAt);
    }

    private void deliver(Long userId, int delaySeconds) {
        try {
            notificationService.createForUserId(
                userId,
                NotificationType.ADMIN_TEST,
                "Тестовое уведомление",
                bodyForDelay(delaySeconds),
                null
            );
            log.info("Delivered admin test notification to user {} after {}s", userId, delaySeconds);
        } catch (Exception ex) {
            log.warn("Failed to deliver admin test notification to user {}", userId, ex);
        }
    }

    private static String bodyForDelay(int delaySeconds) {
        if (delaySeconds == 0) {
            return "Тест из админ-панели — отправлено сразу.";
        }
        return "Тест из админ-панели — отправлено через " + delaySeconds + " сек.";
    }
}
