package com.podpisoff.push;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushServiceFacade pushServiceFacade;

    public PushController(PushServiceFacade pushServiceFacade) {
        this.pushServiceFacade = pushServiceFacade;
    }

    @GetMapping("/vapid-public-key")
    public VapidPublicKeyResponse vapidPublicKey() {
        return pushServiceFacade.publicKey();
    }

    @PostMapping("/subscribe")
    public void subscribe(@Valid @RequestBody PushSubscribeRequest request) {
        pushServiceFacade.subscribe(request);
    }
}
