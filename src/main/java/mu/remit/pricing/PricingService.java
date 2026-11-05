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
        long commission = calculateCommission(sendAmount, scope);
        long margin = calculateMargin(sendAmount, scope);
        long tax = calculateTax(fee + commission + margin, scope);
        long receiveMinor = calculateReceiveAmount(sendAmount, fee, commission, margin, tax, scope);
        double rate = resolveCustomerRate(scope);

        return new Quote(
                quoteId,
                customerId,
                sendAmount,
                new Money(fee, sendAmount.currency()),
                new Money(commission, sendAmount.currency()),
                new Money(margin, sendAmount.currency()),
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
        double customerRate = resolveCustomerRate(scope);
        long grossMinor = (long) Math.round(receiveAmount.minor() / customerRate);
        long fee = calculateFee(new Money(grossMinor, sendCurrency), scope);
        long commission = calculateCommission(new Money(grossMinor, sendCurrency), scope);
        long margin = calculateMargin(new Money(grossMinor, sendCurrency), scope);
        long tax = calculateTax(fee + commission + margin, scope);
        long sendMinor = grossMinor + fee + commission + margin + tax;

        return new Quote(
                quoteId,
                customerId,
                new Money(sendMinor, sendCurrency),
                new Money(fee, sendCurrency),
                new Money(commission, sendCurrency),
                new Money(margin, sendCurrency),
                new Money(tax, sendCurrency),
                receiveAmount,
                Instant.now().plusSeconds(300),
                customerRate);
    }

    public QuoteLimitCheck checkQuoteLimits(String kycLevel, long requestedAmountMinor, long dailyUsedMinor,
            long monthlyUsedMinor) {
        if (kycLevel == null || kycLevel.isBlank()) {
            throw new IllegalArgumentException("kyc level is required");
        }

        long dailyLimit = parseLimit("DAILY_LIMIT", kycLevel);
        long monthlyLimit = parseLimit("MONTHLY_LIMIT", kycLevel);
        long nextDailyTotal = dailyUsedMinor + requestedAmountMinor;
        long nextMonthlyTotal = monthlyUsedMinor + requestedAmountMinor;

        if (nextDailyTotal <= dailyLimit && nextMonthlyTotal <= monthlyLimit) {
            return new QuoteLimitCheck(true, 0L, "Transfer is within the current KYC limits.");
        }

        if (nextDailyTotal > dailyLimit || nextMonthlyTotal > monthlyLimit) {
            return new QuoteLimitCheck(false, Math.max(dailyLimit - dailyUsedMinor, monthlyLimit - monthlyUsedMinor),
                    "Transfer exceeds limit. KYC upgrade is recommended to increase your limits.");
        }

        return new QuoteLimitCheck(true, 0L, "Transfer is within the current KYC limits.");
    }

    public boolean isQuoteActive(Quote quote) {
        if (quote == null) {
            throw new IllegalArgumentException("quote is required");
        }
        return quote.expiresAt().isAfter(Instant.now());
    }

    public Quote refreshQuote(Quote quote) {
        if (quote == null) {
            throw new IllegalArgumentException("quote is required");
        }
        return new Quote(
                quote.id(),
                quote.customerId(),
                quote.sendAmount(),
                quote.feeAmount(),
                quote.commissionAmount(),
                quote.marginAmount(),
                quote.taxAmount(),
                quote.receiveAmount(),
                Instant.now().plusSeconds(300),
                quote.customerRate());
    }

    public RateValidationCheck validateRate(String corridor, String source, Instant fetchedAt, String fallbackSource) {
        if (corridor == null || corridor.isBlank()) {
            throw new IllegalArgumentException("corridor is required");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("rate source is required");
        }
        if (fetchedAt == null) {
            throw new IllegalArgumentException("fetch timestamp is required");
        }

        long maxAgeSeconds = parseLong(
                findParameter("RATE_MAX_AGE_SECONDS", new ParameterScope(corridor, null, null, null), "30"));
        long ageSeconds = Math.max(0, Instant.now().getEpochSecond() - fetchedAt.getEpochSecond());
        if (ageSeconds > maxAgeSeconds) {
            if (fallbackSource == null || fallbackSource.isBlank()) {
                return new RateValidationCheck(false, null,
                        "Rate for corridor " + corridor + " is stale and the corridor is paused.");
            }
            return new RateValidationCheck(false, fallbackSource,
                    "Rate for corridor " + corridor + " is stale; using fallback source " + fallbackSource + ".");
        }

        return new RateValidationCheck(true, source, "Rate source " + source + " is valid.");
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

    private long calculateCommission(Money sendAmount, ParameterScope scope) {
        String commissionRateValue = findParameter("COMMISSION_RATE", scope, "0");
        if (BigDecimal.ZERO.compareTo(new BigDecimal(commissionRateValue)) == 0) {
            return 0L;
        }
        BigDecimal commissionRate = new BigDecimal(commissionRateValue);
        BigDecimal commission = BigDecimal.valueOf(sendAmount.minor())
                .multiply(commissionRate)
                .setScale(0, RoundingMode.HALF_UP);
        return commission.longValue();
    }

    private long calculateMargin(Money sendAmount, ParameterScope scope) {
        String marginRateValue = findParameter("FX_MARGIN_RATE", scope, "0");
        if (BigDecimal.ZERO.compareTo(new BigDecimal(marginRateValue)) == 0) {
            return 0L;
        }
        BigDecimal marginRate = new BigDecimal(marginRateValue);
        BigDecimal margin = BigDecimal.valueOf(sendAmount.minor())
                .multiply(marginRate)
                .setScale(0, RoundingMode.HALF_UP);
        return margin.longValue();
    }

    private long calculateTax(long base, ParameterScope scope) {
        String enabled = parameters.getRequired("TAX_ENABLED", scope);
        if (!"true".equalsIgnoreCase(enabled)) {
            return 0L;
        }
        String rateValue = parameters.getRequired("TAX_RATE", scope);
        BigDecimal taxRate = new BigDecimal(rateValue);
        BigDecimal tax = BigDecimal.valueOf(base)
                .multiply(taxRate)
                .setScale(0, RoundingMode.HALF_UP);
        return tax.longValue();
    }

    private long calculateReceiveAmount(Money sendAmount, long fee, long commission, long margin, long tax,
            ParameterScope scope) {
        String rateValue = parameters.getRequired("FX_RATE", scope);
        BigDecimal rate = new BigDecimal(rateValue);
        BigDecimal gross = BigDecimal.valueOf(sendAmount.minor() - fee - commission - margin - tax);
        return gross.multiply(rate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private double resolveRate(ParameterScope scope) {
        String rateValue = parameters.getRequired("FX_RATE", scope);
        return Double.parseDouble(rateValue);
    }

    private double resolveCustomerRate(ParameterScope scope) {
        double baseRate = resolveRate(scope);
        String marginRateValue = findParameter("FX_MARGIN_RATE", scope, "0");
        BigDecimal marginRate = new BigDecimal(marginRateValue);
        BigDecimal adjustedRate = BigDecimal.valueOf(baseRate).multiply(BigDecimal.ONE.subtract(marginRate));
        return adjustedRate.doubleValue();
    }

    private String findParameter(String key, ParameterScope scope, String defaultValue) {
        try {
            return parameters.getRequired(key, scope);
        } catch (IllegalArgumentException ignored) {
            return defaultValue;
        }
    }

    private long parseLimit(String key, String kycLevel) {
        String value = parameters.getRequired(key, new ParameterScope(null, null, null, kycLevel));
        return Long.parseLong(value.trim());
    }

    private long parseLong(String value) {
        return Long.parseLong(value.trim());
    }
}
