package mu.remit.admin;

public record TransactionSupportMatch(
        String transactionId,
        String reference,
        String status,
        String paymentMethod) {
}
