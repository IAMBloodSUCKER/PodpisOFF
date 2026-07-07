package com.podpisoff.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.podpisoff.common.ApiException;
import org.junit.jupiter.api.Test;

class SubscriptionExtrasTest {

    @Test
    void normalizesBareDomain() {
        assertEquals("https://cursor.com", SubscriptionExtras.normalizeResourceUrl("cursor.com"));
    }

    @Test
    void keepsHttpsUrl() {
        assertEquals("https://netflix.com/account", SubscriptionExtras.normalizeResourceUrl("https://netflix.com/account"));
    }

    @Test
    void blankBecomesNull() {
        assertNull(SubscriptionExtras.normalizeResourceUrl("  "));
    }

    @Test
    void rejectsInvalidUrl() {
        assertThrows(ApiException.class, () -> SubscriptionExtras.normalizeResourceUrl("not a url!!!"));
    }
}
