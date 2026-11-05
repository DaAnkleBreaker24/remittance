package mu.remit.beneficiary;

public record BeneficiaryLimitCheck(boolean allowed, long limit, String message) {
    public BeneficiaryLimitCheck {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
    }
}
