package com.podpisoff.telegram;

import java.time.Instant;

public record TelegramLinkResponse(
    String botUsername,
    String deepLink,
    Instant expiresAt
) {
}
