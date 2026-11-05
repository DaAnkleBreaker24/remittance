package mu.remit.transfer;

public record ComplianceResult(ComplianceOutcome outcome, String reason) {
    public ComplianceResult {
        if (outcome == null) {
            throw new IllegalArgumentException("outcome is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
    }
}
