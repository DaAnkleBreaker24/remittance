package mu.remit.transfer;

import java.time.Instant;
import java.util.Objects;

public record TransferTransaction(
        String id,
        String customerId,
        String quoteId,
        String beneficiaryId,
        String paymentMethod,
        TransferStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public TransferTransaction {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("transaction id is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (quoteId == null || quoteId.isBlank()) {
            throw new IllegalArgumentException("quote id is required");
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
        Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");
    }
}
