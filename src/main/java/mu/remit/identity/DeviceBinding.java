package mu.remit.identity;

public record DeviceBinding(String customerId, String sessionId, String deviceId, String otpChallenge) {
    public DeviceBinding {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("session id is required");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("device id is required");
        }
        if (otpChallenge == null || otpChallenge.isBlank()) {
            throw new IllegalArgumentException("otp challenge is required");
        }
    }
}
