package mu.remit.transfer;

public record StatementExport(
        String customerId,
        String format,
        String content) {
    public StatementExport {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (format == null || format.isBlank()) {
            throw new IllegalArgumentException("format is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
    }
}
