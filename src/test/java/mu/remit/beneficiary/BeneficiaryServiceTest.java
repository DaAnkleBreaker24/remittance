package mu.remit.beneficiary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class BeneficiaryServiceTest {

    @Test
    void createsAndScreensBeneficiaryWithStatusLifecycle() {
        BeneficiaryService service = new BeneficiaryService();

        Beneficiary beneficiary = service.addBeneficiary(
                "customer-200",
                "Rangy Andrianina",
                "MG1234567890",
                "MGA",
                "family_support",
                "SIBLING");

        assertThat(service.requiredFields("MGA")).contains("accountNumber", "bankCode");
        assertThat(beneficiary.status()).isEqualTo(BeneficiaryStatus.PENDING_SCREENING);

        Beneficiary reviewed = service.screen(beneficiary.id(), List.of("SANCTION-HIT"));
        assertThat(reviewed.status()).isEqualTo(BeneficiaryStatus.PENDING_REVIEW);

        Beneficiary active = service.activate(reviewed.id());
        assertThat(active.status()).isEqualTo(BeneficiaryStatus.ACTIVE);
    }

    @Test
    void rejectsBadAccountFormatsAndBlockedSanctions() {
        BeneficiaryService service = new BeneficiaryService();

        assertThatThrownBy(() -> service.addBeneficiary(
                "customer-200",
                "Rangy Andrianina",
                "BAD-ACCOUNT",
                "MGA",
                "family_support",
                "SIBLING"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("account");

        Beneficiary blocked = service.addBeneficiary(
                "customer-201",
                "Nora Raharison",
                "MG9876543210",
                "MGA",
                "rent",
                "FRIEND");

        Beneficiary result = service.screen(blocked.id(), List.of("PEP"));
        assertThat(result.status()).isEqualTo(BeneficiaryStatus.BLOCKED);
    }

    @Test
    void validatesCorridorSpecificFieldsBeforeSaving() {
        BeneficiaryService service = new BeneficiaryService();

        assertThatThrownBy(() -> service.validateFields("MGA", "", "MG1234567890", "Rangy Andrianina"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bank code");

        assertThat(service.validateFields("MGA", "B1", "MG1234567890", "Rangy Andrianina"))
                .isEmpty();
    }

    @Test
    void validatesConfiguredRelationshipAndPurposeValues() {
        BeneficiaryService service = new BeneficiaryService();

        assertThat(service.allowedRelationships()).contains("SIBLING", "FRIEND", "SELF");
        assertThat(service.allowedPurposes()).contains("family_support", "education", "medical");

        assertThatThrownBy(() -> service.validateTransferMetadata("UNKNOWN", "family_support"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relationship");

        assertThatThrownBy(() -> service.validateTransferMetadata("SIBLING", "unknown_purpose"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("purpose");

        assertThat(service.validateTransferMetadata("SIBLING", "family_support")).isEmpty();
    }

    @Test
    void reScreensSensitiveEditsAndSoftDeletesBeneficiary() {
        BeneficiaryService service = new BeneficiaryService();

        Beneficiary beneficiary = service.addBeneficiary(
                "customer-300",
                "Alice Rasoanaivo",
                "MG1111111111",
                "MGA",
                "family_support",
                "SIBLING");

        Beneficiary updated = service.updateBeneficiary(
                beneficiary.id(),
                "Alice Rasoanaivo",
                "MG2222222222",
                "family_support",
                "SIBLING");

        assertThat(updated.status()).isEqualTo(BeneficiaryStatus.PENDING_SCREENING);
        assertThat(updated.accountNumber()).isEqualTo("MG2222222222");

        Beneficiary deleted = service.deleteBeneficiary(beneficiary.id());
        assertThat(deleted.status()).isEqualTo(BeneficiaryStatus.DELETED);
        assertThat(service.getById(beneficiary.id()).status()).isEqualTo(BeneficiaryStatus.DELETED);
    }

    @Test
    void appliesCoolingOffLimitToNewlyAddedBeneficiaries() {
        BeneficiaryService service = new BeneficiaryService();

        Beneficiary beneficiary = service.addBeneficiary(
                "customer-301",
                "Bema Rabe",
                "MG3333333333",
                "MGA",
                "rent",
                "FRIEND");

        BeneficiaryLimitCheck limitCheck = service.checkCoolingOff(beneficiary.id(), 600_000L);

        assertThat(limitCheck.allowed()).isFalse();
        assertThat(limitCheck.message()).contains("cooling-off");
        assertThat(limitCheck.limit()).isEqualTo(500_000L);
    }

    @Test
    void rescreensExistingBeneficiariesOnListUpdate() {
        BeneficiaryService service = new BeneficiaryService();

        Beneficiary beneficiary = service.addBeneficiary(
                "customer-302",
                "Nirina Ranjy",
                "MG4444444444",
                "MGA",
                "education",
                "FRIEND");

        Beneficiary rescreened = service.rescreenOnListUpdate(beneficiary.id(), List.of("PEP"));

        assertThat(rescreened.status()).isEqualTo(BeneficiaryStatus.BLOCKED);
        assertThat(service.latestComplianceAlert(beneficiary.id())).contains("PEP");
    }

    @Test
    void flagsSharedAccountPatternsAsSuspicious() {
        BeneficiaryService service = new BeneficiaryService();

        service.addBeneficiary("customer-401", "A", "MG8888888888", "MGA", "rent", "FRIEND");
        service.addBeneficiary("customer-402", "B", "MG8888888888", "MGA", "rent", "FRIEND");
        service.addBeneficiary("customer-403", "C", "MG8888888888", "MGA", "rent", "FRIEND");

        Beneficiary suspicious = service.addBeneficiary("customer-404", "D", "MG8888888888", "MGA", "rent", "FRIEND");
        BeneficiaryAlert alert = service.checkSuspiciousPatterns(suspicious.id());

        assertThat(alert.flagged()).isTrue();
        assertThat(alert.reason()).contains("shared");
    }
}
