package mu.remit.transfer;

public record ScenarioRule(
        String code,
        String description,
        String threshold,
        String status) {
    public ScenarioRule {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (threshold == null || threshold.isBlank()) {
            throw new IllegalArgumentException("threshold is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
    }
}
