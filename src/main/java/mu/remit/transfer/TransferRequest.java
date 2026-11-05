package mu.remit.transfer;

public record TransferRequest(String id, String customerId, Quote quote, String receiveCurrency) {
    public TransferRequest {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("transfer id is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (quote == null) {
            throw new IllegalArgumentException("quote is required");
        }
        if (receiveCurrency == null || receiveCurrency.isBlank()) {
            throw new IllegalArgumentException("receive currency is required");
        }
    }
}
