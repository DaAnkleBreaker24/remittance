package mu.remit.beneficiary;

public record BeneficiaryAlert(boolean flagged, String reason) {
    public BeneficiaryAlert {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
    }
}
