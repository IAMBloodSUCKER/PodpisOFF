package com.podpisoff.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.user.User;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@EnableConfigurationProperties(PushProperties.class)
public class PushServiceFacade {

    private static final Logger log = LoggerFactory.getLogger(PushServiceFacade.class);

    private final PushProperties pushProperties;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final AuthFacade authFacade;
    private final ObjectMapper objectMapper;
    private final PushService pushService;

    public PushServiceFacade(PushProperties pushProperties,
                             PushSubscriptionRepository pushSubscriptionRepository,
                             AuthFacade authFacade,
                             ObjectMapper objectMapper) {
        this.pushProperties = pushProperties;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.authFacade = authFacade;
        this.objectMapper = objectMapper;
        this.pushService = new PushService();
        if (StringUtils.hasText(pushProperties.publicKey()) && StringUtils.hasText(pushProperties.privateKey())) {
            try {
                pushService.setPublicKey(pushProperties.publicKey());
                pushService.setPrivateKey(pushProperties.privateKey());
                pushService.setSubject(pushProperties.subject());
            } catch (Exception ex) {
                log.warn("Failed to configure Web Push keys", ex);
            }
        }
    }

    public VapidPublicKeyResponse publicKey() {
        if (!StringUtils.hasText(pushProperties.publicKey())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Push notifications are not configured");
        }
        return new VapidPublicKeyResponse(pushProperties.publicKey());
    }

    @Transactional
    public void subscribe(PushSubscribeRequest request) {
        User user = authFacade.getCurrentUser();
        PushSubscription subscription = pushSubscriptionRepository.findByEndpoint(request.endpoint())
            .orElseGet(PushSubscription::new);
        subscription.setUser(user);
        subscription.setEndpoint(request.endpoint());
        subscription.setP256dhKey(request.p256dh());
        subscription.setAuthKey(request.auth());
        pushSubscriptionRepository.save(subscription);
    }

    @Async
    public void sendToUser(Long userId, String title, String body, String tag) {
        if (!StringUtils.hasText(pushProperties.publicKey()) || !StringUtils.hasText(pushProperties.privateKey())) {
            return;
        }

        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByUserId(userId);
        if (subscriptions.isEmpty()) {
            return;
        }

        String payload;
        try {
            Map<String, String> message = new LinkedHashMap<>();
            message.put("title", title);
            message.put("body", body == null ? "" : body);
            message.put("url", "/dashboard");
            message.put("tag", tag == null ? "podpisoff" : tag);
            payload = objectMapper.writeValueAsString(message);
        } catch (Exception ex) {
            log.warn("Failed to serialize push payload for user {}", userId, ex);
            return;
        }

        for (PushSubscription stored : subscriptions) {
            try {
                Subscription subscription = new Subscription();
                subscription.endpoint = stored.getEndpoint();
                subscription.keys = new Subscription.Keys();
                subscription.keys.p256dh = stored.getP256dhKey();
                subscription.keys.auth = stored.getAuthKey();

                HttpResponse response = pushService.send(new Notification(subscription, payload));
                int status = response.getStatusLine().getStatusCode();
                if (status == 404 || status == 410) {
                    pushSubscriptionRepository.deleteByEndpoint(stored.getEndpoint());
                } else if (status < 200 || status >= 300) {
                    log.warn("Push delivery failed for user {} with status {}", userId, status);
                }
            } catch (Exception ex) {
                log.warn("Push delivery failed for user {}", userId, ex);
            }
        }
    }
}
