package mu.remit.reporting;

import java.time.Instant;

public record TransactionReportEntry(
        String reference,
        String customerId,
        String corridor,
        String channel,
        String status,
        long amountMinor,
        String currency,
        Instant createdAt) {
    public TransactionReportEntry {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId is required");
        }
        if (corridor == null || corridor.isBlank()) {
            throw new IllegalArgumentException("corridor is required");
        }
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("channel is required");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
    }
}
