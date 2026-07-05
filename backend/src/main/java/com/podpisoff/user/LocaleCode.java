package com.podpisoff.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LocaleCode {
    RU("ru"),
    EN("en");

    private final String value;

    LocaleCode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static LocaleCode fromValue(String input) {
        for (LocaleCode localeCode : values()) {
            if (localeCode.value.equalsIgnoreCase(input)) {
                return localeCode;
            }
        }
        throw new IllegalArgumentException("Unsupported locale: " + input);
    }
}
