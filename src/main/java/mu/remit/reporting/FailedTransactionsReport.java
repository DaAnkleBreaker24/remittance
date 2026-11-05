package mu.remit.reporting;

import java.util.List;
import java.util.Map;

public record FailedTransactionsReport(List<FailedTransactionEntry> items, Map<String, Long> byBucket) {
    public FailedTransactionsReport {
        if (items == null) {
            throw new IllegalArgumentException("items are required");
        }
        if (byBucket == null) {
            throw new IllegalArgumentException("byBucket are required");
        }
    }
}
