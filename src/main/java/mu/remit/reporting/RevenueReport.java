package mu.remit.reporting;

import java.util.List;

public record RevenueReport(
        List<RevenueReportEntry> entries,
        long totalFeeMinor,
        long totalFxMarginMinor,
        long totalCommissionMinor,
        long totalRevenueMinor) {
    public RevenueReport {
        if (entries == null) {
            throw new IllegalArgumentException("entries are required");
        }
    }
}
