package mu.remit.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import mu.remit.platform.Money;
import mu.remit.platform.ParameterizedService;
import org.junit.jupiter.api.Test;

class TransferExecutionServiceTest {

    @Test
    void createsTransactionWithIdempotencyAndTransitionsThroughPaymentStates() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-101",
                "customer-101",
                new Money(10_000L, "MUR"),
                new Money(100L, "MUR"),
                new Money(0L, "MUR"),
                new Money(9_900L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        TransferTransaction first = service.createTransaction("idemp-001", "customer-101", quote, "beneficiary-99",
                "BANK");
        TransferTransaction duplicate = service.createTransaction("idemp-001", "customer-101", quote, "beneficiary-99",
                "BANK");

        assertThat(first).isEqualTo(duplicate);
        assertThat(first.status()).isEqualTo(TransferStatus.CREATED);

        TransferTransaction confirmed = service.confirmPayin("idemp-001");
        assertThat(confirmed.status()).isEqualTo(TransferStatus.PAYIN_CONFIRMED);

        TransferTransaction payoutSent = service.markPayoutSent("idemp-001");
        assertThat(payoutSent.status()).isEqualTo(TransferStatus.Payout_SENT);

        TransferTransaction paid = service.complete("idemp-001");
        assertThat(paid.status()).isEqualTo(TransferStatus.PAID);
    }
}
