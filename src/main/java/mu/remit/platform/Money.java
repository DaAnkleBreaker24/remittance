package mu.remit.platform;

public record Money(long minor, String currency) {
    public Money {
        if (minor < 0) {
            throw new IllegalArgumentException("minor amount cannot be negative");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        currency = currency.toUpperCase();
    }

    public long asMinorUnits() {
        return minor;
    }
}
