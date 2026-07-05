package com.podpisoff.billing;

import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.user.User;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BillingService {

    private final AuthFacade authFacade;
    private final String yookassaShopId;
    private final String yookassaSecretKey;

    public BillingService(AuthFacade authFacade,
                          @Value("${app.yookassa.shop-id:}") String yookassaShopId,
                          @Value("${app.yookassa.secret-key:}") String yookassaSecretKey) {
        this.authFacade = authFacade;
        this.yookassaShopId = yookassaShopId;
        this.yookassaSecretKey = yookassaSecretKey;
    }

    public BillingStatusResponse status() {
        User user = authFacade.getCurrentUser();
        return new BillingStatusResponse(user.getPlan(), user.getPlanExpiresAt());
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
