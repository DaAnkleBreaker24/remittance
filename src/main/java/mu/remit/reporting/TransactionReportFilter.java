package mu.remit.reporting;

import java.time.Instant;

public record TransactionReportFilter(
        Instant from,
        Instant to,
        String corridor,
        String channel,
        String status,
        String customerId,
        String currency) {
    public TransactionReportFilter {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
    }
}
