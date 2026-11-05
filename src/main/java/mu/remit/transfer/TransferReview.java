package mu.remit.transfer;

import mu.remit.platform.Money;

public record TransferReview(
        String id,
        String customerId,
        String beneficiaryId,
        String paymentMethod,
        Money sendAmount,
        Money feeAmount,
        Money receiveAmount,
        double customerRate) {
    public TransferReview {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("transfer id is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (beneficiaryId == null || beneficiaryId.isBlank()) {
            throw new IllegalArgumentException("beneficiary id is required");
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("payment method is required");
        }
        if (sendAmount == null || feeAmount == null || receiveAmount == null) {
            throw new IllegalArgumentException("summary amounts are required");
        }
    }
}
