package mu.remit.transfer;

public record ProofOfPayment(
        String reference,
        String shareUrl,
        String status) {
    public ProofOfPayment {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference is required");
        }
        if (shareUrl == null || shareUrl.isBlank()) {
            throw new IllegalArgumentException("shareUrl is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
    }
}
