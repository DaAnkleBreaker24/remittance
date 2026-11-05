package mu.remit.reporting;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionReportService {

    public TransactionReport generate(List<TransactionReportEntry> rows, TransactionReportFilter filter) {
        if (rows == null) {
            throw new IllegalArgumentException("rows are required");
        }
        if (filter == null) {
            throw new IllegalArgumentException("filter is required");
        }

        List<TransactionReportEntry> filtered = rows.stream()
                .filter(entry -> !entry.createdAt().isBefore(filter.from()))
                .filter(entry -> !entry.createdAt().isAfter(filter.to()))
                .filter(entry -> filter.corridor() == null || filter.corridor().isBlank()
                        || entry.corridor().equalsIgnoreCase(filter.corridor()))
                .filter(entry -> filter.channel() == null || filter.channel().isBlank()
                        || entry.channel().equalsIgnoreCase(filter.channel()))
                .filter(entry -> filter.status() == null || filter.status().isBlank()
                        || entry.status().equalsIgnoreCase(filter.status()))
                .filter(entry -> filter.customerId() == null || filter.customerId().isBlank()
                        || entry.customerId().equalsIgnoreCase(filter.customerId()))
                .filter(entry -> filter.currency() == null || filter.currency().isBlank()
                        || entry.currency().equalsIgnoreCase(filter.currency()))
                .sorted(Comparator.comparing(TransactionReportEntry::createdAt))
                .toList();

        long total = filtered.stream().mapToLong(TransactionReportEntry::amountMinor).sum();
        return new TransactionReport(filtered, total, filtered.size());
    }

    public ReportExport exportCsv(TransactionReport report) {
        if (report == null) {
            throw new IllegalArgumentException("report is required");
        }

        StringBuilder csv = new StringBuilder();
        csv.append("reference,customerId,corridor,channel,status,amountMinor,currency,createdAt");
        csv.append(System.lineSeparator());

        for (TransactionReportEntry entry : report.entries()) {
            csv.append(entry.reference()).append(',')
                    .append(entry.customerId()).append(',')
                    .append(entry.corridor()).append(',')
                    .append(entry.channel()).append(',')
                    .append(entry.status()).append(',')
                    .append(entry.amountMinor()).append(',')
                    .append(entry.currency()).append(',')
                    .append(entry.createdAt())
                    .append(System.lineSeparator());
        }

        return new ReportExport("CSV", csv.toString());
    }
}
