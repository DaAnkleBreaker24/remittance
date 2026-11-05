package mu.remit.transfer;

import java.time.Instant;

public record AuditEntry(
        String caseId,
        String actor,
        String action,
        String beforeValue,
        String afterValue,
        Instant timestamp) {
    public AuditEntry {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId is required");
        }
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor is required");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action is required");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp is required");
        }
    }
}
