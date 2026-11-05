package mu.remit.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RevenueReportServiceTest {

    @Test
    void buildsRevenueReportByCorridorAndTotalsFeeMarginAndCommission() {
        RevenueReportService service = new RevenueReportService();

        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-30T00:00:00Z");

        List<RevenueReportEntry> rows = List.of(
                new RevenueReportEntry("MU-MG", "MUR", "MGA", 2_000L, 750L, 300L,
                        Instant.parse("2026-11-02T08:00:00Z")),
                new RevenueReportEntry("MU-MG", "MUR", "MGA", 3_000L, 900L, 400L,
                        Instant.parse("2026-11-08T08:00:00Z")),
                new RevenueReportEntry("MU-ZA", "MUR", "ZAR", 1_500L, 400L, 150L,
                        Instant.parse("2026-11-15T08:00:00Z")));

        RevenueReportFilter filter = new RevenueReportFilter(from, to, "MU-MG", null);
        RevenueReport report = service.generate(rows, filter);

        assertThat(report.entries()).hasSize(2);
        assertThat(report.totalFeeMinor()).isEqualTo(5_000L);
        assertThat(report.totalFxMarginMinor()).isEqualTo(1_650L);
        assertThat(report.totalCommissionMinor()).isEqualTo(700L);
        assertThat(report.totalRevenueMinor()).isEqualTo(7_350L);
        assertThat(report.entries().getFirst().corridor()).isEqualTo("MU-MG");
    }
}
