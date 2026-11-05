package mu.remit.profile;

public record ProfileSummary(
        String customerId,
        String kycLevel,
        long dailyLimit,
        long monthlyLimit,
        long dailyUsed,
        long monthlyUsed,
        String language,
        String notificationChannel) {
    public ProfileSummary {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (kycLevel == null || kycLevel.isBlank()) {
            throw new IllegalArgumentException("kyc level is required");
        }
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("language is required");
        }
        if (notificationChannel == null || notificationChannel.isBlank()) {
            throw new IllegalArgumentException("notification channel is required");
        }
    }
}
