package mu.remit.phase0;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import mu.remit.ledger.Account;
import mu.remit.ledger.AccountType;
import mu.remit.ledger.LedgerEntry;
import mu.remit.ledger.LedgerService;
import mu.remit.platform.parameters.ParameterScope;
import mu.remit.platform.parameters.ParameterValue;
import mu.remit.platform.parameters.ParameterizedService;
import org.junit.jupiter.api.Test;

class Phase0FunctionalStoryTest {

    @Test
    void e11ParameterRegistryScopePrecedenceMatchesDeveloperAdminRequirements() {
        ParameterizedService service = new ParameterizedService(List.of(
                new ParameterValue("TAX_ENABLED", "GLOBAL", "false"),
                new ParameterValue("TAX_ENABLED", "CORRIDOR:MGA", "true"),
                new ParameterValue("TAX_ENABLED", "CHANNEL:BANK", "false"),
                new ParameterValue("TAX_ENABLED", "PAYMENT_TYPE:WALLET", "true"),
                new ParameterValue("TAX_ENABLED", "SEGMENT:PLATINUM", "true")));

        assertThat(service.getRequired("TAX_ENABLED", new ParameterScope("MGA", "BANK", "WALLET", "PLATINUM")))
                .isEqualTo("true");
        assertThat(service.getRequired("TAX_ENABLED", new ParameterScope("MGA", "BANK", null, null)))
                .isEqualTo("false");
    }

    @Test
    void e12ImmutableLedgerPostingSupportsTaxAndIdempotentPosting() {
        LedgerService ledger = new LedgerService();
        Account collection = new Account("COLLECTION_MU", "Collection MUR", AccountType.ASSET, "MUR");
        Account feeRevenue = new Account("FEE_REVENUE", "Fee revenue", AccountType.REVENUE, "MUR");
        Account taxPayable = new Account("TAX_PAYABLE", "Tax payable", AccountType.LIABILITY, "MUR");

        var first = ledger.post(
                "E12-POST-1",
                "payin with fee and tax",
                List.of(
                        new LedgerEntry(collection, 10_000L, "MUR", true),
                        new LedgerEntry(feeRevenue, 9_000L, "MUR", false),
                        new LedgerEntry(taxPayable, 1_000L, "MUR", false)));

        assertThat(first.entries()).hasSize(3);
        assertThat(ledger.post(
                "E12-POST-1",
                "duplicate submission",
                List.of(
                        new LedgerEntry(collection, 2_000L, "MUR", true),
                        new LedgerEntry(taxPayable, 2_000L, "MUR", false))))
                .isSameAs(first);
    }
}
