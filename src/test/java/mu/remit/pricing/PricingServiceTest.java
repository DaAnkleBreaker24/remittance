package mu.remit.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import mu.remit.platform.Money;
import mu.remit.platform.ParameterScope;
import mu.remit.platform.ParameterValue;
import mu.remit.platform.ParameterizedService;
import mu.remit.transfer.Quote;
import org.junit.jupiter.api.Test;

class PricingServiceTest {

    @Test
    void createsQuoteFromParameterizedFeeAndTaxRules() {
        ParameterizedService parameters = new ParameterizedService(List.of(
                new ParameterValue("FEE_RATE", "CORRIDOR:MGA", "0.0100"),
                new ParameterValue("TAX_ENABLED", "CORRIDOR:MGA", "true"),
                new ParameterValue("TAX_RATE", "CORRIDOR:MGA", "0.1000"),
                new ParameterValue("FX_RATE", "CORRIDOR:MGA", "1.0000")));

        PricingService pricing = new PricingService(parameters);

        Quote quote = pricing.createQuote(
                "quote-200",
                "customer-200",
                new Money(10_000L, "MUR"),
                "MGA",
                "BANK",
                "WALLET",
                "PLATINUM");

        assertThat(quote.sendAmount()).isEqualTo(new Money(10_000L, "MUR"));
        assertThat(quote.feeAmount()).isEqualTo(new Money(100L, "MUR"));
        assertThat(quote.taxAmount()).isEqualTo(new Money(10L, "MUR"));
        assertThat(quote.receiveAmount()).isEqualTo(new Money(9_890L, "MGA"));
        assertThat(quote.customerRate()).isEqualTo(1.0d);
        assertThat(quote.expiresAt()).isNotNull();
        assertThat(quote.expiresAt()).isAfter(java.time.Instant.now());
        assertThat(pricing.getScope("MGA", "BANK", "WALLET", "PLATINUM"))
                .isEqualTo(new ParameterScope("MGA", "BANK", "WALLET", "PLATINUM"));
    }

    @Test
    void createsQuoteFromReceiveAmountForReverseDirection() {
        ParameterizedService parameters = new ParameterizedService(List.of(
                new ParameterValue("FEE_RATE", "CORRIDOR:MGA", "0.0100"),
                new ParameterValue("TAX_ENABLED", "CORRIDOR:MGA", "true"),
                new ParameterValue("TAX_RATE", "CORRIDOR:MGA", "0.1000"),
                new ParameterValue("FX_RATE", "CORRIDOR:MGA", "1.0000")));

        PricingService pricing = new PricingService(parameters);

        Quote reverse = pricing.createQuoteFromReceiveAmount(
                "quote-201",
                "customer-200",
                new Money(7_500L, "MGA"),
                "MUR",
                "BANK",
                "WALLET",
                "PLATINUM");

        assertThat(reverse.receiveAmount()).isEqualTo(new Money(7_500L, "MGA"));
        assertThat(reverse.sendAmount().currency()).isEqualTo("MUR");
        assertThat(reverse.sendAmount().minor()).isGreaterThan(0L);
        assertThat(reverse.expiresAt()).isAfter(java.time.Instant.now());
    }
}
