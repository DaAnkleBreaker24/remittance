package mu.remit.identity;

public record CustomerCredentials(String customerId, String passwordHash, String salt) {
    public CustomerCredentials {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("password hash is required");
        }
        if (salt == null || salt.isBlank()) {
            throw new IllegalArgumentException("salt is required");
        }
    }
}
