package com.podpisoff.billing;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/status")
    public BillingStatusResponse status() {
        return billingService.status();
    }

    @PostMapping("/create-payment")
    public CreatePaymentResponse createPayment(@RequestBody @Valid CreatePaymentRequest request) {
        return billingService.createPayment(request);
    }
}
