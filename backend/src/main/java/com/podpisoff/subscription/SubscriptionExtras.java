package com.podpisoff.subscription;

import com.podpisoff.common.ApiException;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.http.HttpStatus;

public final class SubscriptionExtras {

    private SubscriptionExtras() {
    }

    public static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static String normalizeResourceUrl(String value) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            return null;
        }
        String url = trimmed.matches("(?i)^https?://.+") ? trimmed : "https://" + trimmed;
        try {
            URI uri = new URI(url);
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid resource URL");
            }
            return url;
        } catch (URISyntaxException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid resource URL");
        }
    }
}
