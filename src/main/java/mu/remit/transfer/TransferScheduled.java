package mu.remit.transfer;

import java.time.Instant;
import mu.remit.platform.Money;

public record TransferScheduled(
        String customerId,
        String beneficiaryId,
        String paymentMethod,
        Money sendAmount,
        Money feeAmount,
        Money receiveAmount,
        double lockedRate,
        String frequency,
        Instant scheduledAt) {
    public TransferScheduled {
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
            throw new IllegalArgumentException("amounts are required");
        }
        if (frequency == null || frequency.isBlank()) {
            throw new IllegalArgumentException("frequency is required");
        }
        if (scheduledAt == null) {
            throw new IllegalArgumentException("scheduled time is required");
        }
    }
}
