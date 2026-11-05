package mu.remit.pricing;

public record QuoteLimitCheck(boolean allowed, long remainingLimit, String message) {
    public QuoteLimitCheck {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
    }
}
