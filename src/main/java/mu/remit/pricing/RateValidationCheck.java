package mu.remit.pricing;

public record RateValidationCheck(boolean allowed, String fallbackSource, String message) {
    public RateValidationCheck {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
    }
}
