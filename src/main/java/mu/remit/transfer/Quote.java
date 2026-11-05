package mu.remit.transfer;

import java.time.Instant;
import mu.remit.platform.Money;

public record Quote(
        String id,
        String customerId,
        Money sendAmount,
        Money feeAmount,
        Money commissionAmount,
        Money marginAmount,
        Money taxAmount,
        Money receiveAmount,
        Instant expiresAt,
        double customerRate) {
    public Quote {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("quote id is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (sendAmount == null || feeAmount == null || commissionAmount == null || marginAmount == null
                || taxAmount == null || receiveAmount == null) {
            throw new IllegalArgumentException("quote money values are required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("quote expiry is required");
        }
    }

    public Quote(String id, String customerId, Money sendAmount, Money feeAmount, Money taxAmount,
            Money receiveAmount, Instant expiresAt, double customerRate) {
        this(id, customerId, sendAmount, feeAmount, new Money(0L, sendAmount.currency()), new Money(0L,
                sendAmount.currency()), taxAmount, receiveAmount, expiresAt, customerRate);
    }
}
