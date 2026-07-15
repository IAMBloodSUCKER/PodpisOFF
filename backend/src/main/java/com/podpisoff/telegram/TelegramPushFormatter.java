package com.podpisoff.telegram;

public final class TelegramPushFormatter {

    private TelegramPushFormatter() {
    }

    public static String format(String title, String body) {
        if (title == null || title.isBlank()) {
            return "";
        }
        StringBuilder message = new StringBuilder(TelegramHtml.bold(title.trim()));
        if (body == null || body.isBlank()) {
            return message.toString();
        }
        message.append("\n\n");
        for (String line : body.strip().split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                message.append('\n');
                continue;
            }
            if (trimmed.startsWith("•")) {
                message.append("• ").append(TelegramHtml.escape(trimmed.substring(1).trim()));
            } else {
                message.append(TelegramHtml.escape(trimmed));
            }
            message.append('\n');
        }
        return message.toString().strip();
    }
}
