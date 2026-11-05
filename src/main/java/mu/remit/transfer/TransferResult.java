package mu.remit.transfer;

public record TransferResult(TransferStatus status, String partnerRequest, boolean posted) {
    public TransferResult {
        if (status == null) {
            throw new IllegalArgumentException("transfer status is required");
        }
        if (partnerRequest == null || partnerRequest.isBlank()) {
            throw new IllegalArgumentException("partner request is required");
        }
    }
}
