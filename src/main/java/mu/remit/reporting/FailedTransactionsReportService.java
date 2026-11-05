package mu.remit.reporting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FailedTransactionsReportService {

    public FailedTransactionsReport generate(List<FailedTransactionEntry> items) {
        if (items == null) {
            throw new IllegalArgumentException("items are required");
        }

        Map<String, Long> byBucket = new LinkedHashMap<>();
        byBucket.put("0-24h", items.stream().filter(item -> item.ageHours() <= 24).count());
        byBucket.put("1-3d", items.stream().filter(item -> item.ageHours() > 24 && item.ageHours() < 72).count());
        byBucket.put("3d+", items.stream().filter(item -> item.ageHours() >= 72).count());

        return new FailedTransactionsReport(items, byBucket);
    }
}
