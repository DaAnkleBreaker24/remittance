package mu.remit.instruments;

public record InstrumentOption(String customerId, String type, String brand, String maskedIdentifier, String reference,
        String status) {
    public InstrumentOption {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        if (maskedIdentifier == null || maskedIdentifier.isBlank()) {
            throw new IllegalArgumentException("masked identifier is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
    }
}
