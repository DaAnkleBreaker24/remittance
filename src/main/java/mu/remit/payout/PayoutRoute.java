package mu.remit.payout;

public record PayoutRoute(String corridorCode, PayoutMethod method, String receiveCurrency) {
    public PayoutRoute {
        if (corridorCode == null || corridorCode.isBlank()) {
            throw new IllegalArgumentException("corridor code is required");
        }
        if (method == null) {
            throw new IllegalArgumentException("method is required");
        }
        if (receiveCurrency == null || receiveCurrency.isBlank()) {
            throw new IllegalArgumentException("receive currency is required");
        }
    }
}
