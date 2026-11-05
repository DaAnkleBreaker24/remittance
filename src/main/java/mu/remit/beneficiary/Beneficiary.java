package mu.remit.beneficiary;

import java.util.UUID;

public record Beneficiary(
        String id,
        String customerId,
        String fullName,
        String accountNumber,
        String currency,
        String purpose,
        String relationship,
        BeneficiaryStatus status) {
    public Beneficiary {
        if (id == null || id.isBlank()) {
            id = "beneficiary-" + UUID.randomUUID();
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("beneficiary name is required");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("account number is required");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("purpose is required");
        }
        if (relationship == null || relationship.isBlank()) {
            throw new IllegalArgumentException("relationship is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
    }
}
