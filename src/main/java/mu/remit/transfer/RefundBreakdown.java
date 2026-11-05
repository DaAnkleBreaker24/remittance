package mu.remit.transfer;

public record RefundBreakdown(
        String idempotencyKey,
        long principalMinor,
        long feeMinor,
        long taxMinor,
        String refundPolicy) {
    public RefundBreakdown {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotency key is required");
        }
        if (refundPolicy == null || refundPolicy.isBlank()) {
            throw new IllegalArgumentException("refund policy is required");
        }
    }
}
