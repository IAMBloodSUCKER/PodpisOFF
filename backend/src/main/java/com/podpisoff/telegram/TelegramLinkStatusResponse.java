package com.podpisoff.telegram;

public record TelegramLinkStatusResponse(
    boolean linked,
    boolean pending,
    String botUsername,
    String deepLink
) {
}
