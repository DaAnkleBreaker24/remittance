package mu.remit.transfer;

public record TransactionStatusUpdate(
        String idempotencyKey,
        String customerStatus,
        String deliveryEstimate,
        boolean pushEnabled) {
    public TransactionStatusUpdate {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotency key is required");
        }
        if (customerStatus == null || customerStatus.isBlank()) {
            throw new IllegalArgumentException("customer status is required");
        }
        if (deliveryEstimate == null || deliveryEstimate.isBlank()) {
            throw new IllegalArgumentException("delivery estimate is required");
        }
    }
}
