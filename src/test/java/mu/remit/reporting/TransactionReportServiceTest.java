package mu.remit.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionReportServiceTest {

    @Test
    void generatesFilteredTransactionReportAndCsvExport() {
        TransactionReportService service = new TransactionReportService();

        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-30T00:00:00Z");

        List<TransactionReportEntry> rows = List.of(
                new TransactionReportEntry("tx-100", "customer-1", "MGA", "BANK", "PAID", 1_000L, "MUR",
                        Instant.parse("2026-11-02T09:00:00Z")),
                new TransactionReportEntry("tx-101", "customer-2", "MGA", "BANK", "FAILED", 2_000L, "MUR",
                        Instant.parse("2026-11-03T09:00:00Z")),
                new TransactionReportEntry("tx-102", "customer-1", "MAD", "CARD", "PAID", 3_000L, "MUR",
                        Instant.parse("2026-11-15T09:00:00Z")));

        TransactionReportFilter filter = new TransactionReportFilter(from, to, "MGA", "BANK", "PAID", null, "MUR");
        TransactionReport report = service.generate(rows, filter);

        assertThat(report.entries()).hasSize(1);
        assertThat(report.entries().getFirst().reference()).isEqualTo("tx-100");
        assertThat(report.totalAmountMinor()).isEqualTo(1_000L);
        assertThat(report.count()).isEqualTo(1L);

        ReportExport export = service.exportCsv(report);
        assertThat(export.format()).isEqualTo("CSV");
        assertThat(export.content()).contains("reference");
        assertThat(export.content()).contains("tx-100");
    }
}
