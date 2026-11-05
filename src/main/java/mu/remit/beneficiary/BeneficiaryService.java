package mu.remit.beneficiary;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BeneficiaryService {
    private final Map<String, Beneficiary> beneficiariesById = new ConcurrentHashMap<>();

    public Beneficiary addBeneficiary(String customerId, String fullName, String accountNumber,
            String currency, String purpose, String relationship) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("beneficiary name is required");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        validateTransferMetadata(relationship, purpose);
        validateAccountFormat(currency, accountNumber);

        Beneficiary beneficiary = new Beneficiary(
                "beneficiary-" + UUID.randomUUID(),
                customerId,
                fullName,
                accountNumber,
                currency,
                purpose,
                relationship,
                BeneficiaryStatus.PENDING_SCREENING);
        beneficiariesById.put(beneficiary.id(), beneficiary);
        return beneficiary;
    }

    public List<String> requiredFields(String corridor) {
        if ("MGA".equalsIgnoreCase(corridor)) {
            return List.of("accountNumber", "bankCode", "recipientName");
        }
        return List.of("accountNumber", "recipientName");
    }

    public List<String> allowedRelationships() {
        return List.of("SELF", "SIBLING", "PARENT", "CHILD", "FRIEND", "BUSINESS", "OTHER");
    }

    public List<String> allowedPurposes() {
        return List.of("family_support", "education", "medical", "rent", "gift", "business", "other");
    }

    public List<String> validateTransferMetadata(String relationship, String purpose) {
        if (relationship == null || relationship.isBlank()) {
            throw new IllegalArgumentException("relationship is required");
        }
        if (!allowedRelationships().contains(relationship.toUpperCase())) {
            throw new IllegalArgumentException("relationship is not in the allowed list");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("purpose is required");
        }
        if (!allowedPurposes().contains(purpose)) {
            throw new IllegalArgumentException("purpose is not in the allowed list");
        }
        return List.of();
    }

    public List<String> validateFields(String corridor, String bankCode, String accountNumber, String recipientName) {
        if ("MGA".equalsIgnoreCase(corridor)) {
            if (bankCode == null || bankCode.isBlank()) {
                throw new IllegalArgumentException("bank code is required for MGA recipients");
            }
            if (accountNumber == null || accountNumber.isBlank()) {
                throw new IllegalArgumentException("account number is required for MGA recipients");
            }
            if (recipientName == null || recipientName.isBlank()) {
                throw new IllegalArgumentException("recipient name is required for MGA recipients");
            }
            if (!accountNumber.matches("^MG\\d{9,15}$")) {
                throw new IllegalArgumentException("invalid MGA account format");
            }
            return List.of();
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("account number is required");
        }
        if (recipientName == null || recipientName.isBlank()) {
            throw new IllegalArgumentException("recipient name is required");
        }
        return List.of();
    }

    public Beneficiary screen(String beneficiaryId, List<String> screeningHits) {
        Beneficiary beneficiary = requireBeneficiary(beneficiaryId);
        if (screeningHits == null || screeningHits.isEmpty()) {
            Beneficiary active = new Beneficiary(
                    beneficiary.id(),
                    beneficiary.customerId(),
                    beneficiary.fullName(),
                    beneficiary.accountNumber(),
                    beneficiary.currency(),
                    beneficiary.purpose(),
                    beneficiary.relationship(),
                    BeneficiaryStatus.ACTIVE);
            beneficiariesById.put(active.id(), active);
            return active;
        }

        if (screeningHits.stream().anyMatch(hit -> hit != null && hit.toUpperCase().contains("PEP"))) {
            Beneficiary blocked = new Beneficiary(
                    beneficiary.id(),
                    beneficiary.customerId(),
                    beneficiary.fullName(),
                    beneficiary.accountNumber(),
                    beneficiary.currency(),
                    beneficiary.purpose(),
                    beneficiary.relationship(),
                    BeneficiaryStatus.BLOCKED);
            beneficiariesById.put(blocked.id(), blocked);
            return blocked;
        }

        if (screeningHits.stream().anyMatch(hit -> hit != null && hit.toUpperCase().contains("SANCTION"))) {
            Beneficiary pendingReview = new Beneficiary(
                    beneficiary.id(),
                    beneficiary.customerId(),
                    beneficiary.fullName(),
                    beneficiary.accountNumber(),
                    beneficiary.currency(),
                    beneficiary.purpose(),
                    beneficiary.relationship(),
                    BeneficiaryStatus.PENDING_REVIEW);
            beneficiariesById.put(pendingReview.id(), pendingReview);
            return pendingReview;
        }

        Beneficiary active = new Beneficiary(
                beneficiary.id(),
                beneficiary.customerId(),
                beneficiary.fullName(),
                beneficiary.accountNumber(),
                beneficiary.currency(),
                beneficiary.purpose(),
                beneficiary.relationship(),
                BeneficiaryStatus.ACTIVE);
        beneficiariesById.put(active.id(), active);
        return active;
    }

    public Beneficiary activate(String beneficiaryId) {
        Beneficiary beneficiary = requireBeneficiary(beneficiaryId);
        Beneficiary active = new Beneficiary(
                beneficiary.id(),
                beneficiary.customerId(),
                beneficiary.fullName(),
                beneficiary.accountNumber(),
                beneficiary.currency(),
                beneficiary.purpose(),
                beneficiary.relationship(),
                BeneficiaryStatus.ACTIVE);
        beneficiariesById.put(active.id(), active);
        return active;
    }

    public Beneficiary updateBeneficiary(String beneficiaryId, String fullName, String accountNumber,
            String purpose, String relationship) {
        Beneficiary beneficiary = requireBeneficiary(beneficiaryId);
        validateTransferMetadata(relationship, purpose);
        validateAccountFormat(beneficiary.currency(), accountNumber);

        Beneficiary updated = new Beneficiary(
                beneficiary.id(),
                beneficiary.customerId(),
                fullName == null || fullName.isBlank() ? beneficiary.fullName() : fullName,
                accountNumber,
                beneficiary.currency(),
                purpose,
                relationship,
                BeneficiaryStatus.PENDING_SCREENING);
        beneficiariesById.put(updated.id(), updated);
        return updated;
    }

    public Beneficiary deleteBeneficiary(String beneficiaryId) {
        Beneficiary beneficiary = requireBeneficiary(beneficiaryId);
        Beneficiary deleted = new Beneficiary(
                beneficiary.id(),
                beneficiary.customerId(),
                beneficiary.fullName(),
                beneficiary.accountNumber(),
                beneficiary.currency(),
                beneficiary.purpose(),
                beneficiary.relationship(),
                BeneficiaryStatus.DELETED);
        beneficiariesById.put(deleted.id(), deleted);
        return deleted;
    }

    public Beneficiary getById(String beneficiaryId) {
        return requireBeneficiary(beneficiaryId);
    }

    public BeneficiaryLimitCheck checkCoolingOff(String beneficiaryId, long transferAmountMinor) {
        Beneficiary beneficiary = requireBeneficiary(beneficiaryId);
        final long coolingOffCap = 500_000L;
        final long coolingOffHours = 24L;

        if (beneficiary.status() == BeneficiaryStatus.PENDING_SCREENING
                || beneficiary.status() == BeneficiaryStatus.PENDING_REVIEW) {
            return new BeneficiaryLimitCheck(false, coolingOffCap,
                    "This beneficiary is in cooling-off. Please wait " + coolingOffHours
                            + " hours before sending more than " + coolingOffCap + " minor units.");
        }

        if (transferAmountMinor > coolingOffCap) {
            return new BeneficiaryLimitCheck(false, coolingOffCap,
                    "New beneficiary cooling-off applies: transfers above " + coolingOffCap
                            + " are temporarily blocked for 24 hours.");
        }

        return new BeneficiaryLimitCheck(true, coolingOffCap, "Transfer allowed within the cooling-off limit.");
    }

    public Beneficiary rescreenOnListUpdate(String beneficiaryId, List<String> listHits) {
        Beneficiary beneficiary = requireBeneficiary(beneficiaryId);
        if (listHits == null || listHits.isEmpty()) {
            return beneficiary;
        }

        if (listHits.stream().anyMatch(hit -> hit != null && hit.toUpperCase().contains("PEP"))) {
            Beneficiary blocked = new Beneficiary(
                    beneficiary.id(),
                    beneficiary.customerId(),
                    beneficiary.fullName(),
                    beneficiary.accountNumber(),
                    beneficiary.currency(),
                    beneficiary.purpose(),
                    beneficiary.relationship(),
                    BeneficiaryStatus.BLOCKED);
            beneficiariesById.put(blocked.id(), blocked);
            complianceAlerts.put(beneficiaryId, "PEP hit identified on list update");
            return blocked;
        }

        if (listHits.stream().anyMatch(hit -> hit != null && hit.toUpperCase().contains("SANCTION"))) {
            Beneficiary pending = new Beneficiary(
                    beneficiary.id(),
                    beneficiary.customerId(),
                    beneficiary.fullName(),
                    beneficiary.accountNumber(),
                    beneficiary.currency(),
                    beneficiary.purpose(),
                    beneficiary.relationship(),
                    BeneficiaryStatus.PENDING_REVIEW);
            beneficiariesById.put(pending.id(), pending);
            complianceAlerts.put(beneficiaryId, "SANCTION hit identified on list update");
            return pending;
        }

        complianceAlerts.put(beneficiaryId, "No list hits after rescreening");
        return beneficiary;
    }

    public String latestComplianceAlert(String beneficiaryId) {
        return complianceAlerts.getOrDefault(beneficiaryId, "No alert");
    }

    public BeneficiaryAlert checkSuspiciousPatterns(String beneficiaryId) {
        Beneficiary beneficiary = requireBeneficiary(beneficiaryId);
        long sharedCount = beneficiariesById.values().stream()
                .filter(candidate -> candidate.accountNumber().equalsIgnoreCase(beneficiary.accountNumber()))
                .count();

        if (sharedCount >= 3) {
            String reason = "shared account pattern detected across " + sharedCount + " customers";
            complianceAlerts.put(beneficiaryId, reason);
            return new BeneficiaryAlert(true, reason);
        }

        return new BeneficiaryAlert(false, "No suspicious beneficiary pattern detected");
    }

    private final Map<String, String> complianceAlerts = new ConcurrentHashMap<>();

    private void validateAccountFormat(String currency, String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("account number is required");
        }
        if ("MGA".equalsIgnoreCase(currency)) {
            if (!accountNumber.matches("^MG\\d{9,15}$")) {
                throw new IllegalArgumentException("invalid MGA account format");
            }
        }
    }

    private Beneficiary requireBeneficiary(String beneficiaryId) {
        if (beneficiaryId == null || beneficiaryId.isBlank()) {
            throw new IllegalArgumentException("beneficiary id is required");
        }
        Beneficiary beneficiary = beneficiariesById.get(beneficiaryId);
        if (beneficiary == null) {
            throw new IllegalArgumentException("beneficiary not found: " + beneficiaryId);
        }
        return beneficiary;
    }
}
