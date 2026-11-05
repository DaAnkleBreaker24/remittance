package mu.remit.transfer;

import java.util.List;

public record TransferDetail(
        String reference,
        String customerStatus,
        List<TransactionTimelineEntry> timeline,
        long sendAmountMinor,
        long feeMinor,
        long receiveAmountMinor,
        double rate,
        String beneficiaryId,
        String paymentMethod) {
    public TransferDetail {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference is required");
        }
        if (customerStatus == null || customerStatus.isBlank()) {
            throw new IllegalArgumentException("customer status is required");
        }
        if (timeline == null) {
            throw new IllegalArgumentException("timeline is required");
        }
    }
}
