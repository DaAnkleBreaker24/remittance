package mu.remit.pricing;

public record PromoCodeValidation(boolean allowed, long discountMinor, String message) {
    public PromoCodeValidation {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
    }
}
