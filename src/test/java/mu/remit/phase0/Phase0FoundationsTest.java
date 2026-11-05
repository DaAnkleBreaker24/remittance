package mu.remit.phase0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import mu.remit.ledger.Account;
import mu.remit.ledger.AccountType;
import mu.remit.ledger.LedgerEntry;
import mu.remit.ledger.LedgerService;
import mu.remit.platform.money.Money;
import mu.remit.platform.parameters.ParameterScope;
import mu.remit.platform.parameters.ParameterValue;
import mu.remit.platform.parameters.ParameterizedService;
import org.junit.jupiter.api.Test;

class Phase0FoundationsTest {

    @Test
    void moneyCapturesMinorUnitsAndCurrency() {
        Money mur = new Money(1250L, "MUR");

        assertThat(mur.minor()).isEqualTo(1250L);
        assertThat(mur.currency()).isEqualTo("MUR");
        assertThat(mur.asMinorUnits()).isEqualTo(1250L);
    }

    @Test
    void parameterServiceResolvesHighestPrecedenceValue() {
        ParameterizedService service = new ParameterizedService(List.of(
                new ParameterValue("FEE_RATE", "GLOBAL", "0.0100"),
                new ParameterValue("FEE_RATE", "CORRIDOR:MGA", "0.0080"),
                new ParameterValue("FEE_RATE", "CHANNEL:BANK", "0.0060"),
                new ParameterValue("FEE_RATE", "PAYMENT_TYPE:WALLET", "0.0040"),
                new ParameterValue("FEE_RATE", "SEGMENT:PLATINUM", "0.0030")));

        assertThat(service.getRequired("FEE_RATE", new ParameterScope("MGA", "BANK", "WALLET", "PLATINUM")))
                .isEqualTo("0.0030");
        assertThat(service.getRequired("FEE_RATE", new ParameterScope("MGA", "BANK", "WALLET", null)))
                .isEqualTo("0.0040");
        assertThat(service.getRequired("FEE_RATE", new ParameterScope("MGA", "BANK", null, null)))
                .isEqualTo("0.0060");
    }

    @Test
    void ledgerRejectsUnbalancedPostings() {
        LedgerService ledger = new LedgerService();
        Account asset = new Account("COLLECTION_MU", "Collection MUR", AccountType.ASSET, "MUR");
        Account revenue = new Account("FEE_REVENUE", "Fees", AccountType.REVENUE, "MUR");

        assertThatThrownBy(() -> ledger.post(
                "ltx-001",
                "fee posting",
                List.of(
                        new LedgerEntry(asset, 1000L, "MUR", true),
                        new LedgerEntry(revenue, 900L, "MUR", false))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("balanced");
    }

    @Test
    void ledgerPostsBalancedDoubleEntry() {
        LedgerService ledger = new LedgerService();
        Account asset = new Account("COLLECTION_MU", "Collection MUR", AccountType.ASSET, "MUR");
        Account feeRevenue = new Account("FEE_REVENUE", "Fees", AccountType.REVENUE, "MUR");

        var posted = ledger.post(
                "ltx-002",
                "fee capture",
                List.of(
                        new LedgerEntry(asset, 1000L, "MUR", true),
                        new LedgerEntry(feeRevenue, 1000L, "MUR", false)));

        assertThat(posted.idempotencyKey()).isEqualTo("ltx-002");
        assertThat(posted.entries()).hasSize(2);
        assertThat(posted.entries()).extracting("account.code").containsExactlyInAnyOrder("COLLECTION_MU",
                "FEE_REVENUE");
    }

    @Test
    void ledgerPreventsDuplicateIdempotencyKeysAndSupportsTaxLine() {
        LedgerService ledger = new LedgerService();
        Account collection = new Account("COLLECTION_MU", "Collection MUR", AccountType.ASSET, "MUR");
        Account feeRevenue = new Account("FEE_REVENUE", "Fees", AccountType.REVENUE, "MUR");
        Account taxPayable = new Account("TAX_PAYABLE", "Tax payable", AccountType.LIABILITY, "MUR");

        var first = ledger.post(
                "ltx-tax-001",
                "payin with tax",
                List.of(
                        new LedgerEntry(collection, 10000L, "MUR", true),
                        new LedgerEntry(feeRevenue, 9000L, "MUR", false),
                        new LedgerEntry(taxPayable, 1000L, "MUR", false)));

        assertThat(first.entries()).hasSize(3);

        var duplicate = ledger.post(
                "ltx-tax-001",
                "duplicate tax posting",
                List.of(
                        new LedgerEntry(collection, 500L, "MUR", true),
                        new LedgerEntry(taxPayable, 500L, "MUR", false)));

        assertThat(duplicate).isSameAs(first);
    }
}
