package com.podpisoff.billing;

import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.user.Plan;
import com.podpisoff.user.PlanLimits;
import com.podpisoff.user.PlanAccessService;
import com.podpisoff.user.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BillingService {

    private final AuthFacade authFacade;
    private final PlanAccessService planAccessService;
    private final String yookassaShopId;
    private final String yookassaSecretKey;

    public BillingService(AuthFacade authFacade,
                          PlanAccessService planAccessService,
                          @Value("${app.yookassa.shop-id:}") String yookassaShopId,
                          @Value("${app.yookassa.secret-key:}") String yookassaSecretKey) {
        this.authFacade = authFacade;
        this.planAccessService = planAccessService;
        this.yookassaShopId = yookassaShopId;
        this.yookassaSecretKey = yookassaSecretKey;
    }

    public BillingStatusResponse status() {
        User user = authFacade.getCurrentUser();
        return new BillingStatusResponse(planAccessService.effectivePlan(user), user.getPlanExpiresAt());
    }

    public List<BillingPlanResponse> plans() {
        return List.of(
            new BillingPlanResponse(
                Plan.FREE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                PlanLimits.FREE_SUBSCRIPTION_LIMIT,
                PlanLimits.FREE_REMINDER_LIMIT,
                List.of(
                    "planFeatureSubs3",
                    "planFeatureDashboard",
                    "planFeatureBillingReminders",
                    "planFeatureCurrencyRub"
                )
            ),
            new BillingPlanResponse(
                Plan.PRO,
                new BigDecimal("149.00"),
                new BigDecimal("1.99"),
                new BigDecimal("1290.00"),
                new BigDecimal("12.99"),
                -1,
                -1,
                List.of(
                    "planFeatureSubsUnlimited",
                    "planFeatureDashboard",
                    "planFeatureBillingReminders",
                    "planFeatureMultiCurrency",
                    "planFeatureCsvExport",
                    "planFeatureAnalytics",
                    "planFeatureSpendingCharts",
                    "planFeatureMonthOverrides",
                    "planFeatureTopSubscriptions"
                )
            )
        );
    }

    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        if (yookassaShopId.isBlank() || yookassaSecretKey.isBlank()) {
            throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "YooKassa is not configured");
        }
        String paymentId = UUID.randomUUID().toString();
        return new CreatePaymentResponse(
            paymentId,
            "https://mock-payments.podpisoff.local/pay/" + paymentId,
            "PENDING"
        );
    }
}
