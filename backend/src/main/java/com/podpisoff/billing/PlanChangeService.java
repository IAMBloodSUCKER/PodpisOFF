package com.podpisoff.billing;

import com.podpisoff.notification.NotificationService;
import com.podpisoff.notification.NotificationType;
import com.podpisoff.user.LocaleCode;
import com.podpisoff.user.Plan;
import com.podpisoff.user.PlanAccessService;
import com.podpisoff.user.User;
import com.podpisoff.user.UserRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanChangeService {

    public enum Source {
        ADMIN,
        DEV_TOOLS
    }

    private final UserRepository userRepository;
    private final PlanAccessService planAccessService;
    private final NotificationService notificationService;

    public PlanChangeService(UserRepository userRepository,
                             PlanAccessService planAccessService,
                             NotificationService notificationService) {
        this.userRepository = userRepository;
        this.planAccessService = planAccessService;
        this.notificationService = notificationService;
    }

    @Transactional
    public User applyPlan(User user, Plan plan, LocalDateTime planExpiresAt, Source source) {
        Plan before = planAccessService.effectivePlan(user);
        user.setPlan(plan);
        if (plan == Plan.FREE) {
            user.setPlanExpiresAt(null);
        } else {
            user.setPlanExpiresAt(planExpiresAt);
        }
        User saved = userRepository.save(user);
        Plan after = planAccessService.effectivePlan(saved);
        if (before != after) {
            notifyPlanChange(saved, after, source);
        }
        return saved;
    }

    private void notifyPlanChange(User user, Plan effectivePlan, Source source) {
        LocaleCode locale = user.getLocale() == null ? LocaleCode.RU : user.getLocale();
        boolean russian = locale == LocaleCode.RU;
        String title;
        String body;
        NotificationType type;
        if (effectivePlan == Plan.PRO) {
            type = NotificationType.PLAN_UPGRADED;
            title = russian ? "🚀 Тариф PRO активирован" : "🚀 PRO plan activated";
            body = russian
                ? "Вам подключён тариф PRO:\n• безлимит подписок\n• любые валюты\n• экспорт в Excel"
                : "Your PRO plan is active:\n• unlimited subscriptions\n• any currency\n• Excel export";
        } else {
            type = NotificationType.PLAN_DOWNGRADED;
            title = russian ? "📉 Тариф изменён на Free" : "📉 Plan changed to Free";
            body = russian
                ? "Тариф PRO отключён:\n• до 3 подписок\n• только рубли"
                : "PRO plan is off:\n• up to 3 subscriptions\n• rubles only";
        }
        if (source == Source.ADMIN) {
            body += russian ? "\n\n👤 Изменение выполнено администратором." : "\n\n👤 Changed by an administrator.";
        }
        notificationService.create(user, type, title, body, null);
    }
}
