package com.podpisoff.admin;

import com.podpisoff.feedback.AdminFeedbackResponse;
import com.podpisoff.feedback.AdminReplyRequest;
import com.podpisoff.feedback.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAccessService adminAccessService;
    private final AdminMetricsService adminMetricsService;
    private final FeedbackService feedbackService;
    private final AdminNotificationTestService adminNotificationTestService;
    private final AdminUserManagementService adminUserManagementService;

    public AdminController(AdminAccessService adminAccessService,
                           AdminMetricsService adminMetricsService,
                           FeedbackService feedbackService,
                           AdminNotificationTestService adminNotificationTestService,
                           AdminUserManagementService adminUserManagementService) {
        this.adminAccessService = adminAccessService;
        this.adminMetricsService = adminMetricsService;
        this.feedbackService = feedbackService;
        this.adminNotificationTestService = adminNotificationTestService;
        this.adminUserManagementService = adminUserManagementService;
    }

    @PostMapping("/verify-key")
    public AdminKeyResponse verifyKey(@Valid @RequestBody AdminKeyRequest request) {
        return new AdminKeyResponse(adminAccessService.verifyKey(request.key()));
    }

    @GetMapping("/metrics")
    public AdminMetricsResponse metrics(HttpServletRequest request) {
        adminAccessService.requireAdminKey(request);
        return adminMetricsService.metrics();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users(HttpServletRequest request,
                                         @RequestParam(required = false) String plan) {
        adminAccessService.requireAdminKey(request);
        return adminMetricsService.users(plan);
    }

    @GetMapping("/feedback")
    public List<AdminFeedbackResponse> feedback(HttpServletRequest request) {
        adminAccessService.requireAdminKey(request);
        return feedbackService.listAllForAdmin();
    }

    @PostMapping("/feedback/{id}/reply")
    public AdminFeedbackResponse reply(HttpServletRequest httpRequest,
                                       @PathVariable Long id,
                                       @Valid @RequestBody AdminReplyRequest request) {
        adminAccessService.requireAdminKey(httpRequest);
        return feedbackService.reply(id, request);
    }

    @PostMapping("/test-notification")
    public AdminTestNotificationResponse testNotification(HttpServletRequest httpRequest,
                                                          @Valid @RequestBody AdminTestNotificationRequest request) {
        return adminNotificationTestService.schedule(httpRequest, request.delaySeconds());
    }

    @PostMapping("/users/{id}/notify")
    public AdminUserResponse notifyUser(HttpServletRequest httpRequest,
                                        @PathVariable Long id,
                                        @Valid @RequestBody AdminNotifyUserRequest request) {
        return adminUserManagementService.notifyUser(httpRequest, id, request);
    }

    @PostMapping("/users/{id}/block")
    public AdminUserResponse blockUser(HttpServletRequest httpRequest,
                                       @PathVariable Long id,
                                       @Valid @RequestBody AdminBlockUserRequest request) {
        return adminUserManagementService.blockUser(httpRequest, id, request);
    }

    @PostMapping("/users/{id}/unblock")
    public AdminUserResponse unblockUser(HttpServletRequest httpRequest, @PathVariable Long id) {
        return adminUserManagementService.unblockUser(httpRequest, id);
    }

    @PostMapping("/users/{id}/plan")
    public AdminUserResponse setUserPlan(HttpServletRequest httpRequest,
                                         @PathVariable Long id,
                                         @Valid @RequestBody AdminSetPlanRequest request) {
        return adminUserManagementService.setUserPlan(httpRequest, id, request);
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(HttpServletRequest httpRequest, @PathVariable Long id) {
        adminUserManagementService.deleteUser(httpRequest, id);
    }
}
