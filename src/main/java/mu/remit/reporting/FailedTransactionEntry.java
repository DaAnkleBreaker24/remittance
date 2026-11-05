package mu.remit.reporting;

public record FailedTransactionEntry(
        String reference,
        String status,
        String reason,
        long ageHours,
        String action) {
    public FailedTransactionEntry {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action is required");
        }
    }
}
