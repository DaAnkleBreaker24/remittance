package mu.remit.profile;

public record CustomerProfile(String customerId, String fullName, String kycLevel, String language,
        String notificationChannel) {
    public CustomerProfile {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("full name is required");
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
