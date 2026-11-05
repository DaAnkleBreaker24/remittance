package mu.remit.reporting;

public record ReconciliationItem(
        String corridor,
        String partner,
        String status,
        long amountMinor,
        long ageHours,
        String reference) {
    public ReconciliationItem {
        if (corridor == null || corridor.isBlank()) {
            throw new IllegalArgumentException("corridor is required");
        }
        if (partner == null || partner.isBlank()) {
            throw new IllegalArgumentException("partner is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference is required");
        }
    }
}
