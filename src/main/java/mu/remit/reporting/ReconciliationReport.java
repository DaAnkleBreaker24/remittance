package mu.remit.reporting;

import java.util.Map;

public record ReconciliationReport(
        long matchedCount,
        long unmatchedCount,
        long totalUnmatchedMinor,
        Map<String, Long> agingBuckets) {
    public ReconciliationReport {
        if (agingBuckets == null) {
            throw new IllegalArgumentException("agingBuckets are required");
        }
    }

    public Map<String, Long> ageingBuckets() {
        return agingBuckets;
    }
}
