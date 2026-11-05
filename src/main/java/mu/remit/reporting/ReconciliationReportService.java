package mu.remit.reporting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReconciliationReportService {

    public ReconciliationReport generate(List<ReconciliationItem> items, String corridor, String partner) {
        if (items == null) {
            throw new IllegalArgumentException("items are required");
        }

        List<ReconciliationItem> filtered = items.stream()
                .filter(item -> corridor == null || corridor.isBlank() || item.corridor().equalsIgnoreCase(corridor))
                .filter(item -> partner == null || partner.isBlank() || item.partner().equalsIgnoreCase(partner))
                .toList();

        long matched = filtered.stream().filter(item -> "MATCHED".equalsIgnoreCase(item.status())).count();
        long unmatched = filtered.stream().filter(item -> "UNMATCHED".equalsIgnoreCase(item.status())).count();
        long unmatchedMinor = filtered.stream()
                .filter(item -> "UNMATCHED".equalsIgnoreCase(item.status()))
                .mapToLong(ReconciliationItem::amountMinor)
                .sum();

        Map<String, Long> aging = new LinkedHashMap<>();
        aging.put("0-24h", filtered.stream().filter(item -> item.ageHours() <= 24).count());
        aging.put("24-48h", filtered.stream().filter(item -> item.ageHours() > 24 && item.ageHours() < 48).count());
        aging.put("48h", filtered.stream().filter(item -> item.ageHours() >= 48).count());

        return new ReconciliationReport(matched, unmatched, unmatchedMinor, aging);
    }
}
