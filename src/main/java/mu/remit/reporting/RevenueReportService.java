package mu.remit.reporting;

import java.util.Comparator;
import java.util.List;

public class RevenueReportService {

    public RevenueReport generate(List<RevenueReportEntry> rows, RevenueReportFilter filter) {
        if (rows == null) {
            throw new IllegalArgumentException("rows are required");
        }
        if (filter == null) {
            throw new IllegalArgumentException("filter is required");
        }

        List<RevenueReportEntry> filtered = rows.stream()
                .filter(entry -> !entry.createdAt().isBefore(filter.from()))
                .filter(entry -> !entry.createdAt().isAfter(filter.to()))
                .filter(entry -> filter.corridor() == null || filter.corridor().isBlank()
                        || entry.corridor().equalsIgnoreCase(filter.corridor()))
                .filter(entry -> filter.currency() == null || filter.currency().isBlank()
                        || entry.sourceCurrency().equalsIgnoreCase(filter.currency()))
                .sorted(Comparator.comparing(RevenueReportEntry::createdAt))
                .toList();

        long feeTotal = filtered.stream().mapToLong(RevenueReportEntry::feeMinor).sum();
        long marginTotal = filtered.stream().mapToLong(RevenueReportEntry::fxMarginMinor).sum();
        long commissionTotal = filtered.stream().mapToLong(RevenueReportEntry::commissionMinor).sum();
        long revenueTotal = feeTotal + marginTotal + commissionTotal;

        return new RevenueReport(filtered, feeTotal, marginTotal, commissionTotal, revenueTotal);
    }
}
