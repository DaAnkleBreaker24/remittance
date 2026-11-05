package mu.remit.identity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IdentityService {
    private final Map<String, CustomerCredentials> credentialsByCustomer = new ConcurrentHashMap<>();
    private final Map<String, String> passwordHashByCustomer = new ConcurrentHashMap<>();
    private final Map<String, CustomerSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> deviceOtpByCustomer = new ConcurrentHashMap<>();
    private final Map<String, String> deviceBindings = new ConcurrentHashMap<>();

    public CustomerCredentials register(String customerId, String password) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password is required");
        }

        String salt = UUID.randomUUID().toString();
        String hash = sha256(password + ":" + salt);
        var credentials = new CustomerCredentials(customerId, hash, salt);
        credentialsByCustomer.put(customerId, credentials);
        passwordHashByCustomer.put(customerId, hash);
        return credentials;
    }

    public CustomerSession login(String customerId, String password) {
        if (!verifyPassword(customerId, password)) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        CustomerSession session = new CustomerSession(customerId, sessionId, now, now.plusSeconds(3600));
        activeSessions.put(sessionId, session);
        return session;
    }

    public boolean verifyPassword(String customerId, String password) {
        if (customerId == null || customerId.isBlank()) {
            return false;
        }
        var credentials = credentialsByCustomer.get(customerId);
        if (credentials == null || password == null || password.isBlank()) {
            return false;
        }
        String expected = sha256(password + ":" + credentials.salt());
        return expected.equals(credentials.passwordHash());
    }

    public boolean validateSession(String sessionId) {
        CustomerSession session = activeSessions.get(sessionId);
        if (session == null) {
            return false;
        }
        return session.expiresAt().isAfter(Instant.now());
    }

    public void revokeSession(String sessionId) {
        activeSessions.remove(sessionId);
    }

    public DeviceBinding bindDevice(String sessionId, String deviceId, String otpChallenge) {
        CustomerSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("session not found");
        }
        String challenge = otpChallenge == null || otpChallenge.isBlank() ? "123456" : otpChallenge;
        deviceOtpByCustomer.computeIfAbsent(session.customerId(), key -> new ConcurrentHashMap<>());
        deviceOtpByCustomer.get(session.customerId()).put(deviceId, challenge);
        String bindingKey = session.customerId() + ":" + deviceId;
        deviceBindings.put(bindingKey, sessionId);
        return new DeviceBinding(session.customerId(), sessionId, deviceId, challenge);
    }

    public boolean isDeviceBound(String customerId, String deviceId) {
        return deviceBindings.containsKey(customerId + ":" + deviceId);
    }

    public boolean verifyDeviceOtp(String customerId, String deviceId, String otp) {
        Map<String, String> otps = deviceOtpByCustomer.get(customerId);
        if (otps == null) {
            return false;
        }
        String stored = otps.get(deviceId);
        return stored != null && stored.equals(otp);
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : encoded) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
