package mu.remit.reporting;

public record LiquidityBalance(String currency, long balanceMinor, long thresholdMinor, double daysOfCover) {
    public LiquidityBalance {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
    }
}
