package com.podpisoff.telegram;

import com.podpisoff.dashboard.DashboardAnalyticsService;
import com.podpisoff.dashboard.DashboardService;
import com.podpisoff.dashboard.DashboardSummaryResponse;
import com.podpisoff.dashboard.DashboardAnalyticsResponse;
import com.podpisoff.export.ExportService;
import com.podpisoff.subscription.Subscription;
import com.podpisoff.subscription.SubscriptionRepository;
import com.podpisoff.subscription.SubscriptionResponse;
import com.podpisoff.subscription.SubscriptionService;
import com.podpisoff.user.Plan;
import com.podpisoff.user.PlanAccessService;
import com.podpisoff.user.User;
import com.podpisoff.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramBotQueryService {

    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final DashboardService dashboardService;
    private final DashboardAnalyticsService dashboardAnalyticsService;
    private final ExportService exportService;
    private final PlanAccessService planAccessService;
    private final String siteUrl;

    public TelegramBotQueryService(UserRepository userRepository,
                                   SubscriptionService subscriptionService,
                                   SubscriptionRepository subscriptionRepository,
                                   DashboardService dashboardService,
                                   DashboardAnalyticsService dashboardAnalyticsService,
                                   ExportService exportService,
                                   PlanAccessService planAccessService,
                                   @Value("${app.site-url:http://localhost:3000}") String siteUrl) {
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
        this.subscriptionRepository = subscriptionRepository;
        this.dashboardService = dashboardService;
        this.dashboardAnalyticsService = dashboardAnalyticsService;
        this.exportService = exportService;
        this.planAccessService = planAccessService;
        this.siteUrl = siteUrl == null || siteUrl.isBlank() ? "http://localhost:3000" : siteUrl.trim();
    }

    public Optional<User> findLinkedUser(String chatId) {
        if (chatId == null || chatId.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByTelegramChatId(chatId.trim())
            .filter(user -> user.getTelegramChatId() != null && user.isTelegramNotificationsEnabled());
    }

    public String notLinkedMessage(boolean russian) {
        return russian
            ? "Аккаунт не подключён. Откройте Настройки на сайте и подключите Telegram."
            : "Account is not linked. Open site settings and connect Telegram.";
    }

    public String siteUrl() {
        return siteUrl;
    }

    public String billingUrl() {
        return siteUrl.replaceAll("/$", "") + "/settings#plans";
    }

    public boolean isPro(User user) {
        return planAccessService.effectivePlan(user) == Plan.PRO;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> subscriptions(User user) {
        return subscriptionService.listForUser(user);
    }

    @Transactional(readOnly = true)
    public int includedSubscriptionCount(User user) {
        subscriptionService.rolloverDueDates(user);
        List<Subscription> all = subscriptionRepository.findAllByUserIdOrderByNextBillingDateAsc(user.getId());
        return planAccessService.includedSubscriptionIds(user, all).size();
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(User user) {
        return dashboardService.summaryForUser(user, null, null);
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(User user, int year, int month) {
        return dashboardService.summaryForUser(user, year, month);
    }

    @Transactional(readOnly = true)
    public DashboardAnalyticsResponse analytics(User user) {
        return dashboardAnalyticsService.analyticsForUser(user, 6);
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(User user) {
        return exportService.exportSubscriptionsExcelForUser(user);
    }
}
