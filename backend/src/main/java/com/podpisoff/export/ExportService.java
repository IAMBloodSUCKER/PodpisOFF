package com.podpisoff.export;

import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.subscription.Subscription;
import com.podpisoff.subscription.SubscriptionRepository;
import com.podpisoff.user.Plan;
import com.podpisoff.user.User;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ExportService {

    private final AuthFacade authFacade;
    private final SubscriptionRepository subscriptionRepository;

    public ExportService(AuthFacade authFacade, SubscriptionRepository subscriptionRepository) {
        this.authFacade = authFacade;
        this.subscriptionRepository = subscriptionRepository;
    }

    public String exportSubscriptionsCsv() {
        User user = authFacade.getCurrentUser();
        if (user.getPlan() != Plan.PRO) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CSV export is available only for PRO plan");
        }
        List<Subscription> subscriptions = subscriptionRepository.findAllByUserIdOrderByNextBillingDateAsc(user.getId());
        StringBuilder sb = new StringBuilder();
        sb.append("id,title,category,amount,currency,nextBillingDate,active\n");
        for (Subscription s : subscriptions) {
            sb.append(s.getId()).append(',')
                .append(escape(s.getTitle())).append(',')
                .append(escape(s.getCategory())).append(',')
                .append(s.getAmount()).append(',')
                .append(s.getCurrency()).append(',')
                .append(s.getNextBillingDate()).append(',')
                .append(s.isActive()).append('\n');
        }
        return sb.toString();
    }

    private String escape(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
