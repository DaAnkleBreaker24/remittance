package mu.remit.transfer;

import java.time.Instant;

public record TransactionTimelineEntry(
        TransferStatus status,
        Instant occurredAt,
        String note) {
    public TransactionTimelineEntry {
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
    }
}
