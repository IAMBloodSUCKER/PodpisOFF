package com.podpisoff.admin;

import com.podpisoff.auth.LoginEventRepository;
import com.podpisoff.billing.PlanChangeService;
import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.dev.DevPlanService;
import com.podpisoff.feedback.FeedbackRepository;
import com.podpisoff.notification.NotificationService;
import com.podpisoff.notification.NotificationType;
import com.podpisoff.notification.UserNotificationRepository;
import com.podpisoff.user.Plan;
import com.podpisoff.user.User;
import com.podpisoff.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserManagementService {

    private final AdminAccessService adminAccessService;
    private final AuthFacade authFacade;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final UserNotificationRepository notificationRepository;
    private final FeedbackRepository feedbackRepository;
    private final LoginEventRepository loginEventRepository;
    private final DevPlanService devPlanService;
    private final AdminMetricsService adminMetricsService;
    private final PlanChangeService planChangeService;

    public AdminUserManagementService(AdminAccessService adminAccessService,
                                      AuthFacade authFacade,
                                      UserRepository userRepository,
                                      NotificationService notificationService,
                                      UserNotificationRepository notificationRepository,
                                      FeedbackRepository feedbackRepository,
                                      LoginEventRepository loginEventRepository,
                                      DevPlanService devPlanService,
                                      AdminMetricsService adminMetricsService,
                                      PlanChangeService planChangeService) {
        this.adminAccessService = adminAccessService;
        this.authFacade = authFacade;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.feedbackRepository = feedbackRepository;
        this.loginEventRepository = loginEventRepository;
        this.devPlanService = devPlanService;
        this.adminMetricsService = adminMetricsService;
        this.planChangeService = planChangeService;
    }

    @Transactional
    public AdminUserResponse notifyUser(HttpServletRequest request, Long userId, AdminNotifyUserRequest body) {
        adminAccessService.requireAdminKey(request);
        User target = requireTargetUser(userId);
        notificationService.create(
            target,
            NotificationType.ADMIN_MESSAGE,
            body.title().trim(),
            body.body() == null ? null : body.body().trim(),
            null
        );
        return adminMetricsService.toUserResponse(target);
    }

    @Transactional
    public AdminUserResponse blockUser(HttpServletRequest request, Long userId, AdminBlockUserRequest body) {
        adminAccessService.requireAdminKey(request);
        User actor = authFacade.getCurrentUser();
        User target = requireTargetUser(userId);
        assertCanModify(actor, target);

        if (body.permanent()) {
            target.setBlockedPermanently(true);
            target.setBlockedUntil(null);
        } else {
            if (body.hours() == null || body.hours() <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "hours must be positive when block is temporary");
            }
            target.setBlockedPermanently(false);
            target.setBlockedUntil(LocalDateTime.now().plusHours(body.hours()));
        }

        userRepository.save(target);
        return adminMetricsService.toUserResponse(target);
    }

    @Transactional
    public AdminUserResponse setUserPlan(HttpServletRequest request, Long userId, AdminSetPlanRequest body) {
        adminAccessService.requireAdminKey(request);
        User actor = authFacade.getCurrentUser();
        User target = requireTargetUser(userId);
        assertCanModify(actor, target);

        LocalDateTime expiresAt = body.plan() == Plan.FREE ? null : body.planExpiresAt();
        planChangeService.applyPlan(target, body.plan(), expiresAt, PlanChangeService.Source.ADMIN);
        return adminMetricsService.toUserResponse(target);
    }

    @Transactional
    public AdminUserResponse unblockUser(HttpServletRequest request, Long userId) {
        adminAccessService.requireAdminKey(request);
        User actor = authFacade.getCurrentUser();
        User target = requireTargetUser(userId);
        assertCanModify(actor, target);

        target.setBlockedPermanently(false);
        target.setBlockedUntil(null);
        userRepository.save(target);
        return adminMetricsService.toUserResponse(target);
    }

    @Transactional
    public void deleteUser(HttpServletRequest request, Long userId) {
        adminAccessService.requireAdminKey(request);
        User actor = authFacade.getCurrentUser();
        User target = requireTargetUser(userId);
        assertCanModify(actor, target);

        notificationRepository.deleteByUserId(target.getId());
        feedbackRepository.deleteByUserId(target.getId());
        loginEventRepository.deleteByUser_Id(target.getId());
        userRepository.delete(target);
    }

    private User requireTargetUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void assertCanModify(User actor, User target) {
        if (actor.getId().equals(target.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot modify your own account");
        }
        if (devPlanService.isDevAdmin(target)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot modify a dev admin account");
        }
    }
}
