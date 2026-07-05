package com.podpisoff.auth;

public record CaptchaResponse(
    String captchaId,
    String question
) {
}
