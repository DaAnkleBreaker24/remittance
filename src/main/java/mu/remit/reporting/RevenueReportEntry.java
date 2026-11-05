package mu.remit.reporting;

import java.time.Instant;

public record RevenueReportEntry(
        String corridor,
        String sourceCurrency,
        String targetCurrency,
        long feeMinor,
        long fxMarginMinor,
        long commissionMinor,
        Instant createdAt) {
    public RevenueReportEntry {
        if (corridor == null || corridor.isBlank()) {
            throw new IllegalArgumentException("corridor is required");
        }
        if (sourceCurrency == null || sourceCurrency.isBlank()) {
            throw new IllegalArgumentException("sourceCurrency is required");
        }
        if (targetCurrency == null || targetCurrency.isBlank()) {
            throw new IllegalArgumentException("targetCurrency is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
    }
}
