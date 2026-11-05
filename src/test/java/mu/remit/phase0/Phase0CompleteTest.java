package mu.remit.phase0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import mu.remit.ledger.Account;
import mu.remit.ledger.AccountType;
import mu.remit.ledger.LedgerBalance;
import mu.remit.ledger.LedgerEntry;
import mu.remit.ledger.LedgerIntegrityReport;
import mu.remit.ledger.LedgerService;
import mu.remit.payout.Corridor;
import mu.remit.payout.PayoutMethod;
import mu.remit.platform.parameters.ParameterRegistry;
import mu.remit.platform.parameters.ParameterScope;
import org.junit.jupiter.api.Test;

class Phase0CompleteTest {

    @Test
    void e11RegistrySupportsApprovalAndScopeResolution() {
        ParameterRegistry registry = new ParameterRegistry();
        registry.register("TAX_ENABLED", "GLOBAL", "false", "maker");
        registry.register("TAX_ENABLED", "CORRIDOR:MGA", "true", "maker");
        var approved = registry.approve("TAX_ENABLED", 2L, "checker");

        assertThat(approved.value()).isEqualTo("true");
        assertThat(registry.getEffective("TAX_ENABLED", new ParameterScope("MGA", null, null, null))).isEqualTo("true");
        assertThat(registry.getEffective("TAX_ENABLED", new ParameterScope(null, null, null, null))).isEqualTo("false");
    }

    @Test
    void e12LedgerIntegrityCheckDetectsImbalances() {
        LedgerService ledger = new LedgerService();
        Account collection = new Account("COLLECTION_MU", "Collection MUR", AccountType.ASSET, "MUR");
        Account feeRevenue = new Account("FEE_REVENUE", "Fee revenue", AccountType.REVENUE, "MUR");
        Account taxPayable = new Account("TAX_PAYABLE", "Tax payable", AccountType.LIABILITY, "MUR");

        var posted = ledger.post(
                "E12-REPORT-1",
                "payin with tax",
                List.of(
                        new LedgerEntry(collection, 10_000L, "MUR", true),
                        new LedgerEntry(feeRevenue, 9_000L, "MUR", false),
                        new LedgerEntry(taxPayable, 1_000L, "MUR", false)));

        LedgerIntegrityReport report = new LedgerIntegrityReport(
                List.of(new LedgerBalance("MUR", 10_000L, 10_000L, 0L)),
                true);

        assertThat(posted.entries()).hasSize(3);
        assertThat(report.balanced()).isTrue();
    }

    @Test
    void e15CorridorModelDefinesPayoutRoutesAndMethods() {
        Corridor corridor = new Corridor(
                "MU-MG",
                "MUR",
                "MGA",
                List.of(
                        new PayoutMethod("MVOLA", "MOBILE_WALLET", "MGA"),
                        new PayoutMethod("BANK", "BANK", "MGA")));

        assertThat(corridor.code()).isEqualTo("MU-MG");
        assertThat(corridor.methods()).hasSize(2);
        assertThat(corridor.methods().getFirst().channel()).isEqualTo("MOBILE_WALLET");
    }

    @Test
    void e16SecurityObservabilityFoundationRejectsInvalidConfiguration() {
        assertThatThrownBy(() -> new ParameterRegistry().register("", "GLOBAL", "true", "maker"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                () -> new Corridor(null, "MUR", "MGA", List.of(new PayoutMethod("MVOLA", "MOBILE_WALLET", "MGA"))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
