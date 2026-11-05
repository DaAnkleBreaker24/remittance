package mu.remit.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SupportConsoleServiceTest {

    @Test
    void searchesCustomersAndTransactionsWithMaskedData() {
        SupportConsoleService service = new SupportConsoleService();
        service.recordCustomer("C-100", "Jean Dupont", "+230 12345678", "MGA-100");
        service.recordTransaction("TX-200", "C-100", "MGA-100", "Payout_SENT", "BANK");

        List<CustomerSupportMatch> customers = service.searchCustomers("12345678");
        assertThat(customers).hasSize(1);
        assertThat(customers.getFirst().phoneMasked()).isEqualTo("******5678");

        List<TransactionSupportMatch> transactions = service.searchTransactions("MGA-100");
        assertThat(transactions).hasSize(1);
        assertThat(transactions.getFirst().reference()).isEqualTo("MGA-100");
        assertThat(transactions.getFirst().status()).isEqualTo("Payout_SENT");
    }

    @Test
    void manualActionsRequireSecondApprovalForMoneyMovementAndAuditTheStep() {
        SupportConsoleService service = new SupportConsoleService();
        service.recordTransaction("TX-200", "C-100", "MGA-100", "Payout_SENT", "BANK");

        assertThatThrownBy(() -> service.performAction("agent-a", "TX-200", "retry", null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("second approval");

        SupportActionResult result = service.performAction("agent-a", "TX-200", "retry", "supervisor-a",
                "supervisor-b");
        assertThat(result.action()).isEqualTo("retry");
        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(result.auditTrail()).hasSize(1);
    }

    @Test
    void refundsFollowPaymentRulesAndCreateReverseLedgerEntries() {
        SupportConsoleService service = new SupportConsoleService();
        service.recordTransaction("TX-300", "C-200", "REF-300", "Payout_SENT", "BANK");

        RefundDecision refund = service.refund("TX-300", "BANK", "agent-a", "supervisor-a", "supervisor-b",
                25_000L, 1_000L, 500L);

        assertThat(refund.policy()).isEqualTo("PRINCIPAL_AND_FEE");
        assertThat(refund.ledgerReversalMinor()).isEqualTo(26_500L);
        assertThat(refund.notification()).contains("refund");
    }
}
