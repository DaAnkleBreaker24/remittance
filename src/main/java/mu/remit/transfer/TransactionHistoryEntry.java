package mu.remit.transfer;

import java.time.Instant;

public record TransactionHistoryEntry(
        String idempotencyKey,
        String beneficiaryId,
        TransferStatus status,
        String customerStatus,
        long sendAmountMinor,
        long feeMinor,
        long receiveAmountMinor,
        Instant createdAt) {
    public TransactionHistoryEntry {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotency key is required");
        }
        if (beneficiaryId == null || beneficiaryId.isBlank()) {
            throw new IllegalArgumentException("beneficiary id is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (customerStatus == null || customerStatus.isBlank()) {
            throw new IllegalArgumentException("customer status is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
    }
}
