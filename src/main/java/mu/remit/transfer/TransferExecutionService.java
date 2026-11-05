package mu.remit.transfer;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mu.remit.platform.ParameterizedService;

public class TransferExecutionService {
    private final Map<String, TransferTransaction> transactions = new ConcurrentHashMap<>();
    private final ParameterizedService parameters;

    public TransferExecutionService(ParameterizedService parameters) {
        this.parameters = parameters;
    }

    public TransferTransaction createTransaction(String idempotencyKey, String customerId, Quote quote,
            String beneficiaryId, String paymentMethod) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotency key is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (quote == null) {
            throw new IllegalArgumentException("quote is required");
        }
        if (beneficiaryId == null || beneficiaryId.isBlank()) {
            throw new IllegalArgumentException("beneficiary id is required");
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("payment method is required");
        }

        return transactions.computeIfAbsent(idempotencyKey, key -> new TransferTransaction(
                key,
                customerId,
                quote.id(),
                beneficiaryId,
                paymentMethod,
                TransferStatus.CREATED,
                Instant.now(),
                Instant.now()));
    }

    public TransferTransaction confirmPayin(String idempotencyKey) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        return updateStatus(tx, TransferStatus.PAYIN_CONFIRMED);
    }

    public TransferTransaction markPayoutSent(String idempotencyKey) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        if (tx.status() != TransferStatus.PAYIN_CONFIRMED) {
            throw new IllegalStateException("payout may only be sent after pay-in confirmation");
        }
        return updateStatus(tx, TransferStatus.Payout_SENT);
    }

    public TransferTransaction complete(String idempotencyKey) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        if (tx.status() != TransferStatus.Payout_SENT) {
            throw new IllegalStateException("transaction must be payout sent before completion");
        }
        return updateStatus(tx, TransferStatus.PAID);
    }

    private TransferTransaction requireTransaction(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotency key is required");
        }
        TransferTransaction tx = transactions.get(idempotencyKey);
        if (tx == null) {
            throw new IllegalArgumentException("transaction not found: " + idempotencyKey);
        }
        return tx;
    }

    private TransferTransaction updateStatus(TransferTransaction tx, TransferStatus nextStatus) {
        TransferTransaction updated = new TransferTransaction(
                tx.id(),
                tx.customerId(),
                tx.quoteId(),
                tx.beneficiaryId(),
                tx.paymentMethod(),
                nextStatus,
                tx.createdAt(),
                Instant.now());
        transactions.put(tx.id(), updated);
        return updated;
    }
}
