package mu.remit.payout;

public record PayoutMethod(String code, String channel, String currency) {
    public PayoutMethod {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("method code is required");
        }
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("channel is required");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
    }
}
