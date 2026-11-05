package mu.remit.reporting;

import java.util.List;

public record TransactionReport(
        List<TransactionReportEntry> entries,
        long totalAmountMinor,
        long count) {
    public TransactionReport {
        if (entries == null) {
            throw new IllegalArgumentException("entries are required");
        }
    }
}
