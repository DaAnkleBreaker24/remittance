package mu.remit.ledger;

import java.util.List;

public record LedgerPostedTransaction(String idempotencyKey, String description, List<LedgerEntry> entries) {
    public LedgerPostedTransaction {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotency key is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("entries are required");
        }
    }
}
