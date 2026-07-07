package com.podpisoff.dev;

import com.podpisoff.auth.AuthResponse;
import com.podpisoff.auth.AuthService;
import com.podpisoff.billing.PlanChangeService;
import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.user.Plan;
import com.podpisoff.user.User;
import com.podpisoff.user.UserRepository;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DevPlanService {

    private final AuthFacade authFacade;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final DevPlanProperties devPlanProperties;
    private final PlanChangeService planChangeService;

    public DevPlanService(AuthFacade authFacade,
                            AuthService authService,
                            UserRepository userRepository,
                            DevPlanProperties devPlanProperties,
                            PlanChangeService planChangeService) {
        this.authFacade = authFacade;
        this.authService = authService;
        this.userRepository = userRepository;
        this.devPlanProperties = devPlanProperties;
        this.planChangeService = planChangeService;
    }

    public DevToolsResponse toolsStatus() {
        User user = authFacade.getCurrentUser();
        return new DevToolsResponse(isDevAdmin(user));
    }

    @Transactional
    public AuthResponse switchPlan(DevPlanSwitchRequest request) {
        User user = authFacade.getCurrentUser();
        requireDevAdmin(user);
        Plan plan = request.plan();
        boolean expired = Boolean.TRUE.equals(request.expired());
        LocalDateTime expiresAt = null;
        if (expired && plan == Plan.PRO) {
            expiresAt = LocalDateTime.now().minusDays(1);
        }
        planChangeService.applyPlan(user, plan, expiresAt, PlanChangeService.Source.DEV_TOOLS);
        return authService.toAuthResponse(user);
    }

    public boolean isDevAdmin(User user) {
        String username = user.getUsername();
        if (username == null || username.isBlank()) {
            return false;
        }
        return devPlanProperties.adminUsernameList().stream()
            .anyMatch(admin -> admin.equalsIgnoreCase(username));
    }

    private void requireDevAdmin(User user) {
        if (!isDevAdmin(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Dev tools are not available");
        }
    }
}
