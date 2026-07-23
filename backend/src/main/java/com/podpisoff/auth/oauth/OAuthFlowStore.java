package com.podpisoff.auth.oauth;

import com.podpisoff.common.ApiException;
import com.podpisoff.user.LocaleCode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OAuthFlowStore {

    private static final long STATE_TTL_SECONDS = 600;
    private static final long TICKET_TTL_SECONDS = 120;

    private final Map<String, StateEntry> states = new ConcurrentHashMap<>();
    private final Map<String, TicketEntry> tickets = new ConcurrentHashMap<>();

    public record StateEntry(LocaleCode locale, String timezone, boolean termsAccepted, Instant expiresAt) {
        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public record TicketEntry(String authJson, Instant expiresAt) {
        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public String createState(LocaleCode locale, String timezone, boolean termsAccepted) {
        cleanup();
        String state = UUID.randomUUID().toString().replace("-", "");
        states.put(state, new StateEntry(
            locale,
            timezone,
            termsAccepted,
            Instant.now().plusSeconds(STATE_TTL_SECONDS)
        ));
        return state;
    }

    public StateEntry consumeState(String state) {
        cleanup();
        if (state == null || state.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid OAuth state");
        }
        StateEntry entry = states.remove(state);
        if (entry == null || entry.expired()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OAuth state expired");
        }
        return entry;
    }

    public String createTicket(String authJson) {
        cleanup();
        String ticket = UUID.randomUUID().toString().replace("-", "");
        tickets.put(ticket, new TicketEntry(authJson, Instant.now().plusSeconds(TICKET_TTL_SECONDS)));
        return ticket;
    }

    public String consumeTicket(String ticket) {
        cleanup();
        if (ticket == null || ticket.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid OAuth ticket");
        }
        TicketEntry entry = tickets.remove(ticket);
        if (entry == null || entry.expired()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OAuth ticket expired");
        }
        return entry.authJson();
    }

    private void cleanup() {
        Instant now = Instant.now();
        states.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
        tickets.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }
}
