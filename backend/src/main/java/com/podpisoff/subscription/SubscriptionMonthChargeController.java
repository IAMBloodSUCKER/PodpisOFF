package com.podpisoff.subscription;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions/{subscriptionId}/month-charges")
public class SubscriptionMonthChargeController {

    private final SubscriptionMonthChargeService chargeService;

    public SubscriptionMonthChargeController(SubscriptionMonthChargeService chargeService) {
        this.chargeService = chargeService;
    }

    @GetMapping
    public List<SubscriptionMonthChargeResponse> list(@PathVariable Long subscriptionId) {
        return chargeService.listForSubscription(subscriptionId);
    }

    @PutMapping
    public SubscriptionMonthChargeResponse upsert(@PathVariable Long subscriptionId,
                                                  @RequestBody @Valid SubscriptionMonthChargeRequest request) {
        return chargeService.upsert(subscriptionId, request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long subscriptionId,
                       @RequestParam int year,
                       @RequestParam int month) {
        chargeService.delete(subscriptionId, year, month);
    }
}
