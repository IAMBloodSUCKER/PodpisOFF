package com.podpisoff.export;

import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.subscription.Subscription;
import com.podpisoff.subscription.SubscriptionRepository;
import com.podpisoff.user.Plan;
import com.podpisoff.user.PlanAccessService;
import com.podpisoff.user.User;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ExportService {

    private static final String PRO_ONLY_MESSAGE = "Export is available only for PRO plan";

    private final AuthFacade authFacade;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanAccessService planAccessService;

    public ExportService(AuthFacade authFacade,
                         SubscriptionRepository subscriptionRepository,
                         PlanAccessService planAccessService) {
        this.authFacade = authFacade;
        this.subscriptionRepository = subscriptionRepository;
        this.planAccessService = planAccessService;
    }

    public String exportSubscriptionsCsv() {
        User user = requireProUser();
        List<Subscription> subscriptions = loadSubscriptions(user);
        return SubscriptionCsvExporter.export(subscriptions, user.getLocale());
    }

    public byte[] exportSubscriptionsExcel() {
        User user = requireProUser();
        return exportSubscriptionsExcelForUser(user);
    }

    public byte[] exportSubscriptionsExcelForUser(User user) {
        if (planAccessService.effectivePlan(user) != Plan.PRO) {
            throw new ApiException(HttpStatus.FORBIDDEN, PRO_ONLY_MESSAGE);
        }
        List<Subscription> subscriptions = loadSubscriptions(user);
        try {
            return SubscriptionExcelExporter.export(subscriptions, user.getLocale());
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build Excel export");
        }
    }

    private User requireProUser() {
        User user = authFacade.getCurrentUser();
        if (planAccessService.effectivePlan(user) != Plan.PRO) {
            throw new ApiException(HttpStatus.FORBIDDEN, PRO_ONLY_MESSAGE);
        }
        return user;
    }

    private List<Subscription> loadSubscriptions(User user) {
        return subscriptionRepository.findAllByUserIdOrderByNextBillingDateAsc(user.getId());
    }
}
