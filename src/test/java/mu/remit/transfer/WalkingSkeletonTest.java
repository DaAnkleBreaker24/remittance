package mu.remit.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import mu.remit.ledger.Account;
import mu.remit.ledger.AccountType;
import mu.remit.ledger.LedgerEntry;
import mu.remit.ledger.LedgerService;
import mu.remit.platform.Money;
import mu.remit.platform.ParameterScope;
import mu.remit.platform.ParameterValue;
import mu.remit.platform.ParameterizedService;
import org.junit.jupiter.api.Test;

class WalkingSkeletonTest {

    @Test
    void quoteAndTransferFlowCreatesBalancedLedgerAndPayoutRequest() {
        ParameterizedService parameters = new ParameterizedService(List.of(
                new ParameterValue("FEE_RATE", "GLOBAL", "0.0100"),
                new ParameterValue("TAX_ENABLED", "GLOBAL", "false")));

        Quote quote = new Quote(
                "quote-001",
                "customer-001",
                new Money(10_000L, "MUR"),
                new Money(100L, "MUR"),
                new Money(0L, "MUR"),
                new Money(9_900L, "MGA"),
                Instant.now().plusSeconds(300),
                1.0d);

        LedgerService ledger = new LedgerService();
        Account collection = new Account("COLLECTION_MU", "Collection MUR", AccountType.ASSET, "MUR");
        Account feeRevenue = new Account("FEE_REVENUE", "Fee revenue", AccountType.REVENUE, "MUR");
        Account customerLiability = new Account("CUSTOMER_LIABILITY", "Customer liability", AccountType.LIABILITY,
                "MUR");
        Account payout = new Account("PAYOUT_IN_TRANSIT", "Payout in transit", AccountType.LIABILITY, "MGA");
        Account partnerSettlement = new Account("FX_POSITION_MGA", "Partner settlement", AccountType.ASSET, "MGA");

        TransferRequest request = new TransferRequest("tx-001", "customer-001", quote, "MGA");
        TransferWorkflow workflow = new TransferWorkflow(parameters, ledger);

        TransferResult result = workflow.execute(request,
                List.of(
                        new LedgerEntry(collection, 10_000L, "MUR", true),
                        new LedgerEntry(customerLiability, 9_900L, "MUR", false),
                        new LedgerEntry(feeRevenue, 100L, "MUR", false)),
                List.of(
                        new LedgerEntry(payout, 9_900L, "MGA", true),
                        new LedgerEntry(partnerSettlement, 9_900L, "MGA", false)));

        assertThat(result.status()).isEqualTo(TransferStatus.PAYIN_CONFIRMED);
        assertThat(result.partnerRequest()).isEqualTo("tx-001");
        assertThat(result.posted()).isTrue();
    }
}
