package mu.remit.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import mu.remit.platform.Money;
import mu.remit.platform.ParameterScope;
import mu.remit.platform.ParameterizedService;
import mu.remit.transfer.Quote;

public class PricingService {
    private final ParameterizedService parameters;

    public PricingService(ParameterizedService parameters) {
        this.parameters = parameters;
    }

    public Quote createQuote(String quoteId, String customerId, Money sendAmount, String receiveCurrency,
            String channel, String paymentType, String segment) {
        if (sendAmount == null) {
            throw new IllegalArgumentException("send amount is required");
        }
        if (quoteId == null || quoteId.isBlank()) {
            throw new IllegalArgumentException("quote id is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }

        ParameterScope scope = getScope(receiveCurrency, channel, paymentType, segment);
        long fee = calculateFee(sendAmount, scope);
        long tax = calculateTax(fee, scope);
        long receiveMinor = calculateReceiveAmount(sendAmount, fee, tax, scope);
        double rate = resolveRate(scope);

        return new Quote(
                quoteId,
                customerId,
                sendAmount,
                new Money(fee, sendAmount.currency()),
                new Money(tax, sendAmount.currency()),
                new Money(receiveMinor, receiveCurrency),
                Instant.now().plusSeconds(300),
                rate);
    }

    public Quote createQuoteFromReceiveAmount(String quoteId, String customerId, Money receiveAmount,
            String sendCurrency, String channel, String paymentType, String segment) {
        if (receiveAmount == null) {
            throw new IllegalArgumentException("receive amount is required");
        }
        if (quoteId == null || quoteId.isBlank()) {
            throw new IllegalArgumentException("quote id is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (sendCurrency == null || sendCurrency.isBlank()) {
            throw new IllegalArgumentException("send currency is required");
        }

        ParameterScope scope = getScope(receiveAmount.currency(), channel, paymentType, segment);
        double rate = resolveRate(scope);
        long grossMinor = (long) Math.round(receiveAmount.minor() / rate);
        long fee = calculateFee(new Money(grossMinor, sendCurrency), scope);
        long tax = calculateTax(fee, scope);
        long sendMinor = grossMinor + fee + tax;

        return new Quote(
                quoteId,
                customerId,
                new Money(sendMinor, sendCurrency),
                new Money(fee, sendCurrency),
                new Money(tax, sendCurrency),
                receiveAmount,
                Instant.now().plusSeconds(300),
                rate);
    }

    public ParameterScope getScope(String corridor, String channel, String paymentType, String segment) {
        return new ParameterScope(corridor, channel, paymentType, segment);
    }

    private long calculateFee(Money sendAmount, ParameterScope scope) {
        String feeRateValue = parameters.getRequired("FEE_RATE", scope);
        BigDecimal feeRate = new BigDecimal(feeRateValue);
        BigDecimal fee = BigDecimal.valueOf(sendAmount.minor())
                .multiply(feeRate)
                .setScale(0, RoundingMode.HALF_UP);
        return fee.longValue();
    }

    private long calculateTax(long fee, ParameterScope scope) {
        String enabled = parameters.getRequired("TAX_ENABLED", scope);
        if (!"true".equalsIgnoreCase(enabled)) {
            return 0L;
        }
        String rateValue = parameters.getRequired("TAX_RATE", scope);
        BigDecimal taxRate = new BigDecimal(rateValue);
        BigDecimal tax = BigDecimal.valueOf(fee)
                .multiply(taxRate)
                .setScale(0, RoundingMode.HALF_UP);
        return tax.longValue();
    }

    private long calculateReceiveAmount(Money sendAmount, long fee, long tax, ParameterScope scope) {
        String rateValue = parameters.getRequired("FX_RATE", scope);
        BigDecimal rate = new BigDecimal(rateValue);
        BigDecimal gross = BigDecimal.valueOf(sendAmount.minor() - fee - tax);
        return gross.multiply(rate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private double resolveRate(ParameterScope scope) {
        String rateValue = parameters.getRequired("FX_RATE", scope);
        return Double.parseDouble(rateValue);
    }
}
