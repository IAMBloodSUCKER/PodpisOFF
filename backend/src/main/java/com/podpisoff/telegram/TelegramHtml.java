package com.podpisoff.telegram;

final class TelegramHtml {

    private TelegramHtml() {
    }

    static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    static String bold(String text) {
        return "<b>" + escape(text) + "</b>";
    }

    static String labelLine(String emoji, String label, String value) {
        return emoji + " " + bold(label) + ": " + escape(value);
    }
}
