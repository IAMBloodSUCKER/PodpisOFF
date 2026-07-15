package com.podpisoff.telegram;

public final class TelegramSecrets {

    private static final String REDACTED_TOKEN = "bot****";

    private TelegramSecrets() {
    }

    public static String redact(String value, String token) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String sanitized = value;
        if (token != null && !token.isBlank()) {
            sanitized = sanitized.replace(token, REDACTED_TOKEN);
        }
        return sanitized.replaceAll("bot\\d+:[A-Za-z0-9_-]+", REDACTED_TOKEN);
    }

    public static String safeErrorMessage(Throwable error, String token) {
        if (error == null) {
            return "unknown error";
        }
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return redact(message, token);
    }
}
