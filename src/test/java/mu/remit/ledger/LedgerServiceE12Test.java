package mu.remit.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LedgerServiceE12Test {

    @Test
    void integrityCheckReportsBalancedAndUnbalancedCurrencies() {
        LedgerService ledger = new LedgerService();
        Account collection = new Account("COLLECTION_MU", "Collection MUR", AccountType.ASSET, "MUR");
        Account feeRevenue = new Account("FEE_REVENUE", "Fees", AccountType.REVENUE, "MUR");
        Account taxPayable = new Account("TAX_PAYABLE", "Tax payable", AccountType.LIABILITY, "MUR");

        LedgerIntegrityReport balanced = ledger.checkIntegrity(List.of(
                new LedgerEntry(collection, 10_000L, "MUR", true),
                new LedgerEntry(feeRevenue, 9_000L, "MUR", false),
                new LedgerEntry(taxPayable, 1_000L, "MUR", false)));

        assertThat(balanced.balanced()).isTrue();
        assertThat(balanced.balances()).hasSize(1);
        assertThat(balanced.balances().getFirst().currency()).isEqualTo("MUR");
        assertThat(balanced.balances().getFirst().net()).isZero();

        LedgerIntegrityReport unbalanced = ledger.checkIntegrity(List.of(
                new LedgerEntry(collection, 10_000L, "MUR", true),
                new LedgerEntry(feeRevenue, 8_500L, "MUR", false)));

        assertThat(unbalanced.balanced()).isFalse();
        assertThat(unbalanced.balances().getFirst().net()).isEqualTo(1_500L);
    }
}
