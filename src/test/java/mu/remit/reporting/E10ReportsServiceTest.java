package mu.remit.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class E10ReportsServiceTest {

    @Test
    void buildsDailyReconciliationReportWithMatchedAndUnmatchedAging() {
        ReconciliationReportService service = new ReconciliationReportService();

        List<ReconciliationItem> items = List.of(
                new ReconciliationItem("MGA", "BANK", "MATCHED", 2_500L, 12L, "payin-1"),
                new ReconciliationItem("MGA", "BANK", "UNMATCHED", 5_000L, 48L, "payin-2"),
                new ReconciliationItem("MGA", "BANK", "MATCHED", 3_250L, 8L, "payin-3"));

        ReconciliationReport report = service.generate(items, "MGA", "BANK");

        assertThat(report.matchedCount()).isEqualTo(2);
        assertThat(report.unmatchedCount()).isEqualTo(1);
        assertThat(report.totalUnmatchedMinor()).isEqualTo(5_000L);
        assertThat(report.ageingBuckets().get("48h")).isEqualTo(1L);
    }

    @Test
    void buildsLiquidityAndFailureReportsForOpsWorkflows() {
        LiquidityReportService liquidity = new LiquidityReportService();
        FailedTransactionsReportService failures = new FailedTransactionsReportService();

        List<LiquidityBalance> balances = List.of(
                new LiquidityBalance("MUR", 25_000L, 10_000L, 4.0d),
                new LiquidityBalance("MGA", 90_000L, 30_000L, 3.0d));

        LiquidityReport liquidityReport = liquidity.generate(balances);
        assertThat(liquidityReport.totalBalanceMinor()).isEqualTo(115_000L);
        assertThat(liquidityReport.daysOfCover().get("MUR")).isEqualTo(4.0d);

        List<FailedTransactionEntry> failed = List.of(
                new FailedTransactionEntry("tx-500", "Payout sent", "partner timeout", 52L, "retry"),
                new FailedTransactionEntry("tx-501", "Payin pending", "manual review", 10L, "investigate"));

        FailedTransactionsReport failedReport = failures.generate(failed);
        assertThat(failedReport.items()).hasSize(2);
        assertThat(failedReport.byBucket().containsKey("1-3d")).isTrue();
        assertThat(failedReport.byBucket().get("1-3d")).isEqualTo(1L);
    }
}
