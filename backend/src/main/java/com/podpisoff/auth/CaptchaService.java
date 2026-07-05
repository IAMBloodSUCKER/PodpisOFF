package com.podpisoff.auth;

import com.podpisoff.common.ApiException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CaptchaService {

    private final Map<String, CaptchaChallenge> storage = new ConcurrentHashMap<>();

    public CaptchaResponse issueCaptcha() {
        int a = ThreadLocalRandom.current().nextInt(1, 20);
        int b = ThreadLocalRandom.current().nextInt(1, 20);
        String id = UUID.randomUUID().toString();
        storage.put(id, new CaptchaChallenge(String.valueOf(a + b), Instant.now().plusSeconds(900)));
        return new CaptchaResponse(id, a + " + " + b + " = ?");
    }

    public void verify(String captchaId, String answer) {
        CaptchaChallenge challenge = storage.get(captchaId);
        if (challenge == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Captcha expired");
        }
        if (challenge.expiresAt().isBefore(Instant.now())) {
            storage.remove(captchaId);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Captcha expired");
        }
        if (!challenge.answer().equals(answer == null ? "" : answer.trim())) {
            storage.remove(captchaId);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Captcha mismatch");
        }
        storage.remove(captchaId);
    }

    private record CaptchaChallenge(String answer, Instant expiresAt) {
    }
}
