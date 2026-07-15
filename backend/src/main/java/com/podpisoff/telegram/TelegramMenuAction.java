package com.podpisoff.telegram;

enum TelegramMenuAction {
    HOME("m:h"),
    SUBS("m:s"),
    UPCOMING("m:u"),
    SUMMARY("m:y"),
    SUMMARY_PREV("m:yp"),
    SUMMARY_NEXT("m:yn"),
    ANALYTICS("m:a"),
    EXPORT("m:x"),
    PLAN("m:p"),
    DISABLE("m:d");

    private final String callbackData;

    TelegramMenuAction(String callbackData) {
        this.callbackData = callbackData;
    }

    String callbackData() {
        return callbackData;
    }

    static TelegramMenuAction fromCallback(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        for (TelegramMenuAction action : values()) {
            if (action.callbackData.equals(data)) {
                return action;
            }
        }
        return null;
    }
}
