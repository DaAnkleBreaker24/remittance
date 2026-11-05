package mu.remit.reporting;

import java.time.Instant;

public record RevenueReportFilter(Instant from, Instant to, String corridor, String currency) {
    public RevenueReportFilter {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
    }
}
