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

    @Test
    void feeCommissionAndMarginAreCalculatedFromParameters() {
        ParameterizedService parameters = new ParameterizedService(List.of(
                new ParameterValue("FEE_TYPE", "CORRIDOR:MGA", "PERCENTAGE"),
                new ParameterValue("FEE_RATE", "CORRIDOR:MGA", "0.0100"),
                new ParameterValue("FEE_MIN", "CORRIDOR:MGA", "25"),
                new ParameterValue("FEE_MAX", "CORRIDOR:MGA", "500"),
                new ParameterValue("COMMISSION_TYPE", "CORRIDOR:MGA", "PERCENTAGE"),
                new ParameterValue("COMMISSION_RATE", "CORRIDOR:MGA", "0.0025"),
                new ParameterValue("FX_MARGIN_RATE", "CORRIDOR:MGA", "0.0050"),
                new ParameterValue("TAX_ENABLED", "CORRIDOR:MGA", "false"),
                new ParameterValue("FX_RATE", "CORRIDOR:MGA", "1.0000")));

        PricingService pricing = new PricingService(parameters);

        Quote quote = pricing.createQuote(
                "quote-pricing-1",
                "customer-200",
                new Money(10_000L, "MUR"),
                "MGA",
                "BANK",
                "WALLET",
                "PLATINUM");

        assertThat(quote.feeAmount()).isEqualTo(new Money(100L, "MUR"));
        assertThat(quote.commissionAmount()).isEqualTo(new Money(25L, "MUR"));
        assertThat(quote.marginAmount()).isEqualTo(new Money(50L, "MUR"));
        assertThat(quote.customerRate()).isEqualTo(0.995d);
        assertThat(quote.taxAmount()).isEqualTo(new Money(0L, "MUR"));
    }

    @Test
    void preChecksQuoteLimitsAndSuggestsKycUpgradeWhenNeeded() {
        ParameterizedService parameters = new ParameterizedService(List.of(
                new ParameterValue("DAILY_LIMIT", "SEGMENT:PLATINUM", "2000000"),
                new ParameterValue("MONTHLY_LIMIT", "SEGMENT:PLATINUM", "5000000"),
                new ParameterValue("DAILY_LIMIT", "SEGMENT:BASIC", "500000"),
                new ParameterValue("MONTHLY_LIMIT", "SEGMENT:BASIC", "1000000")));

        PricingService pricing = new PricingService(parameters);

        var allowed = pricing.checkQuoteLimits("PLATINUM", 1_000_000L, 500_000L, 2_000_000L);
        assertThat(allowed.allowed()).isTrue();
        assertThat(allowed.message()).contains("within");

        var blocked = pricing.checkQuoteLimits("BASIC", 1_000_000L, 400_000L, 800_000L);
        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.message()).contains("KYC upgrade");
    }

    @Test
    void staleRatesAreRejectedAndFallbackSourceUsedWhenAvailable() {
        ParameterizedService parameters = new ParameterizedService(List.of(
                new ParameterValue("RATE_MAX_AGE_SECONDS", "GLOBAL", "30"),
                new ParameterValue("RATE_SOURCE_FALLBACK", "CORRIDOR:MGA", "market-feed")));

        PricingService pricing = new PricingService(parameters);

        var stale = pricing.validateRate("MGA", "partner-feed", java.time.Instant.now().minusSeconds(90),
                "market-feed");
        assertThat(stale.allowed()).isFalse();
        assertThat(stale.fallbackSource()).isEqualTo("market-feed");
        assertThat(stale.message()).contains("stale");

        var paused = pricing.validateRate("MGA", "partner-feed", java.time.Instant.now().minusSeconds(90), "");
        assertThat(paused.allowed()).isFalse();
        assertThat(paused.message()).contains("paused");
    }

    @Test
    void paymentMethodPricingUsesPaymentTypeScope() {
        ParameterizedService parameters = new ParameterizedService(List.of(
                new ParameterValue("FEE_RATE", "GLOBAL", "0.0100"),
                new ParameterValue("FEE_RATE", "PAYMENT_TYPE:CARD", "0.0200"),
                new ParameterValue("TAX_ENABLED", "GLOBAL", "false"),
                new ParameterValue("FX_RATE", "GLOBAL", "1.0000")));

        PricingService pricing = new PricingService(parameters);

        Quote quote = pricing.createQuote(
                "quote-payment-1",
                "customer-200",
                new Money(10_000L, "MUR"),
                "MGA",
                "BANK",
                "CARD",
                "PLATINUM");

        assertThat(quote.feeAmount()).isEqualTo(new Money(200L, "MUR"));
        assertThat(quote.receiveAmount()).isEqualTo(new Money(9_800L, "MGA"));
        assertThat(pricing.getScope("MGA", "BANK", "CARD", "PLATINUM").paymentType()).isEqualTo("CARD");
    }

    @Test
    void promoCodeChecksValidityAndUsageCapBeforeDiscount() {
        ParameterizedService parameters = new ParameterizedService(List.of(
                new ParameterValue("PROMO:WELCOME10:ACTIVE", "CORRIDOR:MGA", "true"),
                new ParameterValue("PROMO:WELCOME10:DISCOUNT_RATE", "CORRIDOR:MGA", "0.1000"),
                new ParameterValue("PROMO:WELCOME10:USAGE_LIMIT", "CORRIDOR:MGA", "50"),
                new ParameterValue("PROMO:WELCOME10:MAX_DISCOUNT", "CORRIDOR:MGA", "500"),
                new ParameterValue("TAX_ENABLED", "GLOBAL", "false"),
                new ParameterValue("FX_RATE", "GLOBAL", "1.0000")));

        PricingService pricing = new PricingService(parameters);

        var valid = pricing.validatePromoCode("WELCOME10", "MGA", "BANK", "CARD", "PLATINUM", 10L);
        assertThat(valid.allowed()).isTrue();
        assertThat(valid.discountMinor()).isEqualTo(100L);
        assertThat(valid.message()).contains("applied");

        var exhausted = pricing.validatePromoCode("WELCOME10", "MGA", "BANK", "CARD", "PLATINUM", 60L);
        assertThat(exhausted.allowed()).isFalse();
        assertThat(exhausted.message()).contains("usage");
    }

    @Test
    void taxCalculationUsesScopedParametersAndConfiguredBase() {
        ParameterizedService parameters = new ParameterizedService(List.of(
                new ParameterValue("FEE_RATE", "CORRIDOR:MGA", "0.0100"),
                new ParameterValue("COMMISSION_RATE", "CORRIDOR:MGA", "0.0025"),
                new ParameterValue("FX_MARGIN_RATE", "CORRIDOR:MGA", "0.0050"),
                new ParameterValue("TAX_ENABLED", "CORRIDOR:MGA", "true"),
                new ParameterValue("TAX_RATE", "CORRIDOR:MGA", "0.1000"),
                new ParameterValue("TAX_BASE", "CORRIDOR:MGA", "FEE"),
                new ParameterValue("TAX_INCLUSIVE", "CORRIDOR:MGA", "false"),
                new ParameterValue("FX_RATE", "CORRIDOR:MGA", "1.0000")));

        PricingService pricing = new PricingService(parameters);

        Quote quote = pricing.createQuote(
                "quote-tax-1",
                "customer-200",
                new Money(10_000L, "MUR"),
                "MGA",
                "BANK",
                "WALLET",
                "PLATINUM");

        assertThat(quote.feeAmount()).isEqualTo(new Money(100L, "MUR"));
        assertThat(quote.commissionAmount()).isEqualTo(new Money(25L, "MUR"));
        assertThat(quote.marginAmount()).isEqualTo(new Money(50L, "MUR"));
        assertThat(quote.taxAmount()).isEqualTo(new Money(10L, "MUR"));
    }

    @Test
    void rateLockExpiresAndCanBeRefreshed() {
        ParameterizedService parameters = new ParameterizedService(List.of(
                new ParameterValue("FEE_RATE", "CORRIDOR:MGA", "0.0100"),
                new ParameterValue("TAX_ENABLED", "CORRIDOR:MGA", "true"),
                new ParameterValue("TAX_RATE", "CORRIDOR:MGA", "0.1000"),
                new ParameterValue("FX_RATE", "CORRIDOR:MGA", "1.0000")));

        PricingService pricing = new PricingService(parameters);

        Quote quote = pricing.createQuote(
                "quote-lock-1",
                "customer-200",
                new Money(10_000L, "MUR"),
                "MGA",
                "BANK",
                "WALLET",
                "PLATINUM");

        assertThat(pricing.isQuoteActive(quote)).isTrue();

        Quote expired = new Quote(
                "quote-lock-2",
                "customer-200",
                new Money(10_000L, "MUR"),
                new Money(100L, "MUR"),
                new Money(10L, "MUR"),
                new Money(9_890L, "MGA"),
                java.time.Instant.now().minusSeconds(5),
                1.0d);

        assertThat(pricing.isQuoteActive(expired)).isFalse();

        Quote refreshed = pricing.refreshQuote(quote);
        assertThat(refreshed.expiresAt()).isAfter(quote.expiresAt());
        assertThat(pricing.isQuoteActive(refreshed)).isTrue();
    }
}
