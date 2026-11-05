package mu.remit.identity;

import java.time.Instant;

public record CustomerSession(String customerId, String sessionId, Instant issuedAt, Instant expiresAt) {
    public CustomerSession {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("session id is required");
        }
        if (issuedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("session timestamps are required");
        }
    }
}
