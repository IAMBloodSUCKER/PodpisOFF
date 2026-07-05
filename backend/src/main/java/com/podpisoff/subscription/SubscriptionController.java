package com.podpisoff.subscription;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public List<SubscriptionResponse> list() {
        return subscriptionService.list();
    }

    @PostMapping
    public SubscriptionResponse create(@RequestBody @Valid SubscriptionRequest request) {
        return subscriptionService.create(request);
    }

    @PutMapping("/{id}")
    public SubscriptionResponse update(@PathVariable Long id, @RequestBody @Valid SubscriptionRequest request) {
        return subscriptionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        subscriptionService.delete(id);
    }
}
