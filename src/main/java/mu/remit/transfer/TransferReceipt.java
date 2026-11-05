package mu.remit.transfer;

import mu.remit.platform.Money;

public record TransferReceipt(
        String reference,
        String customerId,
        String beneficiaryId,
        String paymentMethod,
        TransferStatus status,
        Money sendAmount,
        Money feeAmount,
        Money receiveAmount,
        double customerRate) {
    public TransferReceipt {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference is required");
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
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (sendAmount == null || feeAmount == null || receiveAmount == null) {
            throw new IllegalArgumentException("amounts are required");
        }
    }
}
