package mu.remit.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import mu.remit.platform.Money;
import mu.remit.platform.ParameterizedService;
import org.junit.jupiter.api.Test;

class TransferExecutionServiceTest {

    @Test
    void createsTransactionWithIdempotencyAndTransitionsThroughPaymentStates() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-101",
                "customer-101",
                new Money(10_000L, "MUR"),
                new Money(100L, "MUR"),
                new Money(0L, "MUR"),
                new Money(9_900L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        TransferTransaction first = service.createTransaction("idemp-001", "customer-101", quote, "beneficiary-99",
                "BANK");
        TransferTransaction duplicate = service.createTransaction("idemp-001", "customer-101", quote, "beneficiary-99",
                "BANK");

        assertThat(first).isEqualTo(duplicate);
        assertThat(first.status()).isEqualTo(TransferStatus.CREATED);

        TransferTransaction confirmed = service.confirmPayin("idemp-001");
        assertThat(confirmed.status()).isEqualTo(TransferStatus.PAYIN_CONFIRMED);

        TransferTransaction payoutSent = service.markPayoutSent("idemp-001");
        assertThat(payoutSent.status()).isEqualTo(TransferStatus.Payout_SENT);

        TransferTransaction paid = service.complete("idemp-001");
        assertThat(paid.status()).isEqualTo(TransferStatus.PAID);
    }

    @Test
    void idempotencyKeyReturnsTheSameTransactionForDuplicateCreateAttempts() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-303",
                "customer-303",
                new Money(30_000L, "MUR"),
                new Money(300L, "MUR"),
                new Money(0L, "MUR"),
                new Money(29_700L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        Quote differentQuote = new Quote(
                "quote-304",
                "customer-303",
                new Money(31_000L, "MUR"),
                new Money(310L, "MUR"),
                new Money(0L, "MUR"),
                new Money(30_690L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        TransferTransaction first = service.createTransaction("idemp-003", "customer-303", quote, "beneficiary-77",
                "BANK");
        TransferTransaction duplicate = service.createTransaction("idemp-003", "customer-303", differentQuote,
                "beneficiary-77", "BANK");

        assertThat(duplicate).isEqualTo(first);
        assertThat(duplicate.status()).isEqualTo(TransferStatus.CREATED);
    }

    @Test
    void initiatesPayinByPaymentTypeWithPendingStateAndTimeoutHandling() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-404",
                "customer-404",
                new Money(40_000L, "MUR"),
                new Money(400L, "MUR"),
                new Money(0L, "MUR"),
                new Money(39_600L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        service.createTransaction("idemp-004", "customer-404", quote, "beneficiary-66", "BANK");
        service.confirmReview("idemp-004", true);

        TransferTransaction initiated = service.initiatePayin("idemp-004", "BANK");
        assertThat(initiated.status()).isEqualTo(TransferStatus.PAYIN_PENDING);

        TransferTransaction timedOut = service.markPayinTimedOut("idemp-004");
        assertThat(timedOut.status()).isEqualTo(TransferStatus.FAILED);
    }

    @Test
    void preparesReviewSummaryAndRequiresStepUpAuthToConfirm() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-202",
                "customer-202",
                new Money(20_000L, "MUR"),
                new Money(200L, "MUR"),
                new Money(0L, "MUR"),
                new Money(19_800L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        TransferTransaction created = service.createTransaction("idemp-002", "customer-202", quote, "beneficiary-88",
                "BANK");

        TransferReview review = service.reviewTransfer("idemp-002");

        assertThat(review.beneficiaryId()).isEqualTo("beneficiary-88");
        assertThat(review.sendAmount().minor()).isEqualTo(20_000L);
        assertThat(review.feeAmount().minor()).isEqualTo(200L);
        assertThat(review.paymentMethod()).isEqualTo("BANK");
        assertThat(review.customerRate()).isEqualTo(1.0d);

        assertThatThrownBy(() -> service.confirmReview("idemp-002", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("step-up");

        TransferTransaction confirmed = service.confirmReview("idemp-002", true);
        assertThat(confirmed.status()).isEqualTo(TransferStatus.QUOTED);
        assertThat(created.status()).isEqualTo(TransferStatus.CREATED);
    }

    @Test
    void runsTransactionComplianceChecksAndClassifiesRisk() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-606",
                "customer-606",
                new Money(60_000L, "MUR"),
                new Money(600L, "MUR"),
                new Money(0L, "MUR"),
                new Money(59_400L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        service.createTransaction("idemp-006", "customer-606", quote, "beneficiary-66", "BANK");
        service.confirmReview("idemp-006", true);
        service.initiatePayin("idemp-006", "BANK");

        ComplianceResult pass = service.runComplianceCheck("idemp-006", "MGA", 60_000L, List.of());
        assertThat(pass.outcome()).isEqualTo(ComplianceOutcome.PASS);

        ComplianceResult hold = service.runComplianceCheck("idemp-006", "MGA", 600_000L, List.of("velocity"));
        assertThat(hold.outcome()).isEqualTo(ComplianceOutcome.HOLD);

        ComplianceResult reject = service.runComplianceCheck("idemp-006", "MGA", 60_000L, List.of("SANCTION"));
        assertThat(reject.outcome()).isEqualTo(ComplianceOutcome.REJECT);
        assertThat(service.getTransaction("idemp-006").status()).isEqualTo(TransferStatus.FAILED);
    }

    @Test
    void dispatchesPayoutThroughPartnerIdempotentlyAndStoresReference() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-707",
                "customer-707",
                new Money(70_000L, "MUR"),
                new Money(700L, "MUR"),
                new Money(0L, "MUR"),
                new Money(69_300L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        service.createTransaction("idemp-007", "customer-707", quote, "beneficiary-77", "BANK");
        service.confirmReview("idemp-007", true);
        service.initiatePayin("idemp-007", "BANK");
        service.confirmPayin("idemp-007");

        TransferTransaction firstDispatch = service.dispatchPayout("idemp-007", "MAUCAS", "partner-ref-707");
        TransferTransaction duplicateDispatch = service.dispatchPayout("idemp-007", "MAUCAS", "partner-ref-707");

        assertThat(firstDispatch.status()).isEqualTo(TransferStatus.Payout_SENT);
        assertThat(duplicateDispatch).isEqualTo(firstDispatch);
        assertThat(service.getPartnerReference("idemp-007")).isEqualTo("partner-ref-707");
        assertThat(service.getTransaction("idemp-007").status()).isEqualTo(TransferStatus.Payout_SENT);
    }

    @Test
    void handlesPayoutSuccessFailureAndUnknownResults() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-808",
                "customer-808",
                new Money(80_000L, "MUR"),
                new Money(800L, "MUR"),
                new Money(0L, "MUR"),
                new Money(79_200L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        service.createTransaction("idemp-008", "customer-808", quote, "beneficiary-88", "BANK");
        service.confirmReview("idemp-008", true);
        service.initiatePayin("idemp-008", "BANK");
        service.confirmPayin("idemp-008");
        service.dispatchPayout("idemp-008", "MAUCAS", "partner-ref-808");

        TransferTransaction paid = service.handlePayoutResult("idemp-008", "SUCCESS", "partner-ref-808");
        assertThat(paid.status()).isEqualTo(TransferStatus.PAID);

        Quote unknownQuote = new Quote(
                "quote-809",
                "customer-809",
                new Money(90_000L, "MUR"),
                new Money(900L, "MUR"),
                new Money(0L, "MUR"),
                new Money(89_100L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        service.createTransaction("idemp-009", "customer-809", unknownQuote, "beneficiary-99", "BANK");
        service.confirmReview("idemp-009", true);
        service.initiatePayin("idemp-009", "BANK");
        service.confirmPayin("idemp-009");
        service.dispatchPayout("idemp-009", "MAUCAS", "partner-ref-809");

        TransferTransaction unknown = service.handlePayoutResult("idemp-009", "UNKNOWN", "partner-ref-809");
        assertThat(unknown.status()).isEqualTo(TransferStatus.Payout_SENT);

        Quote failedQuote = new Quote(
                "quote-810",
                "customer-810",
                new Money(100_000L, "MUR"),
                new Money(1_000L, "MUR"),
                new Money(0L, "MUR"),
                new Money(99_000L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        service.createTransaction("idemp-010", "customer-810", failedQuote, "beneficiary-100", "BANK");
        service.confirmReview("idemp-010", true);
        service.initiatePayin("idemp-010", "BANK");
        service.confirmPayin("idemp-010");
        service.dispatchPayout("idemp-010", "MAUCAS", "partner-ref-810");

        TransferTransaction failed = service.handlePayoutResult("idemp-010", "FAILED", "partner-ref-810");
        assertThat(failed.status()).isEqualTo(TransferStatus.FAILED);
    }

    @Test
    void refundsCustomerWhenPayoutFails() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-911",
                "customer-911",
                new Money(110_000L, "MUR"),
                new Money(1_100L, "MUR"),
                new Money(0L, "MUR"),
                new Money(108_900L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        service.createTransaction("idemp-011", "customer-911", quote, "beneficiary-111", "BANK");
        service.confirmReview("idemp-011", true);
        service.initiatePayin("idemp-011", "BANK");
        service.confirmPayin("idemp-011");
        service.dispatchPayout("idemp-011", "MAUCAS", "partner-ref-911");

        TransferTransaction refund = service.refundTransfer("idemp-011", "original-instrument");
        assertThat(refund.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(service.getTransaction("idemp-011").status()).isEqualTo(TransferStatus.FAILED);
    }

    @Test
    void cancelsTransferBeforePayoutAndTriggersCustomerRefund() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-912",
                "customer-912",
                new Money(120_000L, "MUR"),
                new Money(1_200L, "MUR"),
                new Money(0L, "MUR"),
                new Money(118_800L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        service.createTransaction("idemp-012", "customer-912", quote, "beneficiary-112", "BANK");
        service.confirmReview("idemp-012", true);
        service.initiatePayin("idemp-012", "BANK");

        TransferTransaction cancelled = service.cancelTransfer("idemp-012", "customer-requested");
        assertThat(cancelled.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(service.getTransaction("idemp-012").status()).isEqualTo(TransferStatus.FAILED);
    }

    @Test
    void generatesReceiptForCompletedTransfer() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-913",
                "customer-913",
                new Money(130_000L, "MUR"),
                new Money(1_300L, "MUR"),
                new Money(0L, "MUR"),
                new Money(128_700L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        service.createTransaction("idemp-013", "customer-913", quote, "beneficiary-113", "BANK");
        service.confirmReview("idemp-013", true);
        service.initiatePayin("idemp-013", "BANK");
        service.confirmPayin("idemp-013");
        service.dispatchPayout("idemp-013", "MAUCAS", "partner-ref-913");
        service.handlePayoutResult("idemp-013", "SUCCESS", "partner-ref-913");

        TransferReceipt receipt = service.generateReceipt("idemp-013");
        assertThat(receipt.reference()).isEqualTo("idemp-013");
        assertThat(receipt.status()).isEqualTo(TransferStatus.PAID);
        assertThat(receipt.sendAmount().minor()).isEqualTo(130_000L);
        assertThat(receipt.feeAmount().minor()).isEqualTo(1_300L);
    }

    @Test
    void detectsDuplicateTransfersWithinTheTimeWindow() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-914",
                "customer-914",
                new Money(140_000L, "MUR"),
                new Money(1_400L, "MUR"),
                new Money(0L, "MUR"),
                new Money(138_600L, "MGA"),
                Instant.now().minusSeconds(30),
                1.0d);

        service.createTransaction("idemp-014", "customer-914", quote, "beneficiary-114", "BANK");

        assertThat(service.isDuplicateTransfer("customer-914", "beneficiary-114", 140_000L, Instant.now())).isTrue();
        assertThat(service.isDuplicateTransfer("customer-914", "beneficiary-114", 150_000L, Instant.now())).isFalse();
        assertThat(service.isDuplicateTransfer("customer-914", "beneficiary-115", 140_000L, Instant.now())).isFalse();
        assertThat(service.isDuplicateTransfer("customer-914", "beneficiary-114", 140_000L,
                Instant.now().plusSeconds(60 * 20))).isFalse();
    }

    @Test
    void retriesFailedPayoutOnAlternatePartner() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-915",
                "customer-915",
                new Money(150_000L, "MUR"),
                new Money(1_500L, "MUR"),
                new Money(0L, "MUR"),
                new Money(148_500L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        service.createTransaction("idemp-015", "customer-915", quote, "beneficiary-115", "BANK");
        service.confirmReview("idemp-015", true);
        service.initiatePayin("idemp-015", "BANK");
        service.confirmPayin("idemp-015");
        service.dispatchPayout("idemp-015", "MAUCAS", "partner-ref-915");
        service.handlePayoutResult("idemp-015", "FAILED", "partner-ref-915");

        TransferTransaction retried = service.retryPayout("idemp-015", "BANK", 3);
        assertThat(retried.status()).isEqualTo(TransferStatus.Payout_SENT);
        assertThat(service.getPayoutAttempts("idemp-015")).isEqualTo(1);
    }

    @Test
    void schedulesRecurringTransferAndLocksRateForExecution() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-916",
                "customer-916",
                new Money(160_000L, "MUR"),
                new Money(1_600L, "MUR"),
                new Money(0L, "MUR"),
                new Money(158_400L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        TransferScheduled scheduled = service.scheduleTransfer("customer-916", "beneficiary-116", "BANK", quote,
                "MONTHLY");
        assertThat(scheduled.customerId()).isEqualTo("customer-916");
        assertThat(scheduled.frequency()).isEqualTo("MONTHLY");
        assertThat(scheduled.lockedRate()).isEqualTo(1.0d);
        assertThat(service.getScheduledTransfer("customer-916").frequency()).isEqualTo("MONTHLY");
    }

    @Test
    void reversesFeeAndTaxAccordingToConfiguredRefundPolicy() {
        ParameterizedService parameters = new ParameterizedService(List.of(
                new mu.remit.platform.ParameterValue("REFUND_POLICY", "PAYMENT_TYPE:BANK", "PRINCIPAL_AND_FEE"),
                new mu.remit.platform.ParameterValue("TAX_ENABLED", "GLOBAL", "true")));
        TransferExecutionService service = new TransferExecutionService(parameters);

        Quote quote = new Quote(
                "quote-917",
                "customer-917",
                new Money(170_000L, "MUR"),
                new Money(1_700L, "MUR"),
                new Money(0L, "MUR"),
                new Money(0L, "MUR"),
                new Money(170L, "MUR"),
                new Money(168_300L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        service.createTransaction("idemp-016", "customer-917", quote, "beneficiary-116", "BANK");
        service.confirmReview("idemp-016", true);
        service.initiatePayin("idemp-016", "BANK");
        service.confirmPayin("idemp-016");
        service.dispatchPayout("idemp-016", "MAUCAS", "partner-ref-917");

        RefundBreakdown refund = service.calculateRefundBreakdown("idemp-016", "BANK");
        assertThat(refund.principalMinor()).isEqualTo(170_000L);
        assertThat(refund.feeMinor()).isEqualTo(1_700L);
        assertThat(refund.taxMinor()).isEqualTo(170L);
        assertThat(refund.refundPolicy()).isEqualTo("PRINCIPAL_AND_FEE");

        TransferTransaction cancelled = service.cancelTransfer("idemp-016", "customer-requested", "BANK");
        assertThat(cancelled.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(service.getRefundBreakdown("idemp-016").principalMinor()).isEqualTo(170_000L);
    }

    @Test
    void listsTransactionsWithFiltersAndShowsTimelineAndCustomerStatus() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote paidQuote = new Quote(
                "quote-918",
                "customer-918",
                new Money(180_000L, "MUR"),
                new Money(1_800L, "MUR"),
                new Money(0L, "MUR"),
                new Money(178_200L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);
        service.createTransaction("idemp-017", "customer-918", paidQuote, "beneficiary-118", "BANK");
        service.confirmReview("idemp-017", true);
        service.initiatePayin("idemp-017", "BANK");
        service.confirmPayin("idemp-017");
        service.dispatchPayout("idemp-017", "MAUCAS", "partner-ref-918");
        service.handlePayoutResult("idemp-017", "SUCCESS", "partner-ref-918");

        Quote cancelledQuote = new Quote(
                "quote-919",
                "customer-918",
                new Money(190_000L, "MUR"),
                new Money(1_900L, "MUR"),
                new Money(0L, "MUR"),
                new Money(188_100L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);
        service.createTransaction("idemp-018", "customer-918", cancelledQuote, "beneficiary-119", "BANK");
        service.confirmReview("idemp-018", true);
        service.cancelTransfer("idemp-018", "customer-requested");

        List<TransactionHistoryEntry> filtered = service.listTransactions("customer-918", "FAILED", "beneficiary-119",
                null, null, 0, 20);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).beneficiaryId()).isEqualTo("beneficiary-119");
        assertThat(filtered.get(0).customerStatus()).isEqualTo("Failed");

        List<TransactionHistoryEntry> all = service.listTransactions("customer-918", null, null, null, null, 0, 20);
        assertThat(all).hasSize(2);
        assertThat(all.get(0).idempotencyKey()).isEqualTo("idemp-018");
        assertThat(all.get(1).idempotencyKey()).isEqualTo("idemp-017");

        TransferDetail detail = service.getTransactionDetail("idemp-017");
        assertThat(detail.reference()).isEqualTo("idemp-017");
        assertThat(detail.customerStatus()).isEqualTo("Delivered");
        assertThat(detail.timeline()).extracting(TransactionTimelineEntry::status)
                .contains(TransferStatus.CREATED, TransferStatus.QUOTED, TransferStatus.PAYIN_PENDING,
                        TransferStatus.PAYIN_CONFIRMED, TransferStatus.Payout_SENT, TransferStatus.PAID);
        assertThat(service.mapCustomerStatus(TransferStatus.PAID)).isEqualTo("Delivered");
    }

    @Test
    void supportsRealtimeUpdatesHistoryReplayProofSharingAndStatementExport() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-920",
                "customer-920",
                new Money(200_000L, "MUR"),
                new Money(2_000L, "MUR"),
                new Money(0L, "MUR"),
                new Money(198_000L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        service.createTransaction("idemp-019", "customer-920", quote, "beneficiary-120", "BANK");
        service.confirmReview("idemp-019", true);
        service.initiatePayin("idemp-019", "BANK");
        service.confirmPayin("idemp-019");
        service.dispatchPayout("idemp-019", "MAUCAS", "partner-ref-920");
        service.handlePayoutResult("idemp-019", "SUCCESS", "partner-ref-920");

        TransactionStatusUpdate update = service.getRealtimeStatusUpdate("idemp-019");
        assertThat(update.customerStatus()).isEqualTo("Delivered");
        assertThat(update.deliveryEstimate()).contains("Delivered");

        TransferTransaction repeated = service.sendAgainFromHistory("customer-920", "idemp-019");
        assertThat(repeated.customerId()).isEqualTo("customer-920");
        assertThat(repeated.beneficiaryId()).isEqualTo("beneficiary-120");

        ProofOfPayment proof = service.shareProofOfPayment("idemp-019");
        assertThat(proof.shareUrl()).contains("proof");
        assertThat(proof.reference()).isEqualTo("idemp-019");

        StatementExport export = service.exportStatement("customer-920", Instant.now().minusSeconds(86_400),
                Instant.now(), "CSV");
        assertThat(export.customerId()).isEqualTo("customer-920");
        assertThat(export.format()).isEqualTo("CSV");
        assertThat(export.content()).contains("idemp-019");
    }

    @Test
    void managesComplianceCaseQueueByPriorityAssignmentAndFilters() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-921",
                "customer-921",
                new Money(210_000L, "MUR"),
                new Money(2_100L, "MUR"),
                new Money(0L, "MUR"),
                new Money(207_900L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        service.createTransaction("idemp-020", "customer-921", quote, "beneficiary-121", "BANK");
        service.confirmReview("idemp-020", true);
        service.initiatePayin("idemp-020", "BANK");
        service.runComplianceCheck("idemp-020", "MGA", 210_000L, List.of("velocity"));

        ComplianceCase queued = service.createComplianceCase("idemp-020", "TRANSACTION", "HIGH", "customer-921");
        assertThat(queued.caseId()).contains("CASE");
        assertThat(queued.priority()).isEqualTo("HIGH");
        assertThat(queued.status()).isEqualTo("OPEN");
        assertThat(queued.slaHours()).isGreaterThan(0);

        service.assignComplianceCase(queued.caseId(), "analyst-7");
        assertThat(service.getComplianceCase(queued.caseId()).assignee()).isEqualTo("analyst-7");

        List<ComplianceCase> filtered = service.listComplianceCases("TRANSACTION", "HIGH", "analyst-7", 0, 20);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).caseId()).isEqualTo(queued.caseId());

        ComplianceCase next = service.nextComplianceCase();
        assertThat(next.caseId()).isEqualTo(queued.caseId());
    }

    @Test
    void supportsFourEyesDecisionsRescreeningCaseFilesAuditTrailAndRiskControls() {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        ComplianceCase caseToReview = service.createComplianceCase("idemp-021", "TRANSACTION", "HIGH", "customer-921");

        assertThatThrownBy(() -> service.approveComplianceCase(caseToReview.caseId(), "analyst-1", "WATCHLIST"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("second approver");

        ComplianceCase approved = service.approveComplianceCase(caseToReview.caseId(), "analyst-1", "analyst-2",
                "WATCHLIST");
        assertThat(approved.status()).isEqualTo("APPROVED");
        assertThat(approved.decisionReason()).isEqualTo("WATCHLIST");

        ComplianceCase rescreen = service.rescreenCustomer("customer-921", "v2");
        assertThat(rescreen.type()).isEqualTo("RESCREEN");
        assertThat(service.getCustomer360("customer-921").customerId()).isEqualTo("customer-921");

        CasePack pack = service.exportCasePack(caseToReview.caseId(), "customer-921");
        assertThat(pack.customerId()).isEqualTo("customer-921");
        assertThat(pack.entries()).isNotEmpty();

        service.flagSuspiciousActivity("customer-921", "manual review");
        assertThat(service.isCustomerBlocked("customer-921")).isFalse();

        service.blockCustomer("customer-921", "AML risk", 24L);
        assertThat(service.isCustomerBlocked("customer-921")).isTrue();

        List<AuditEntry> audit = service.listComplianceAuditTrail(caseToReview.caseId());
        assertThat(audit).isNotEmpty();

        List<ScenarioRule> rules = service.getScenarioRules();
        assertThat(rules).isNotEmpty();

        service.allowCustomer("customer-921", "review cleared");
        assertThat(service.isCustomerBlocked("customer-921")).isFalse();
    }

    @Test
    void verifiesWebhookSignatureDeduplicatesAndConfirmsPendingPayin()
            throws NoSuchAlgorithmException, InvalidKeyException {
        TransferExecutionService service = new TransferExecutionService(new ParameterizedService(List.of()));

        Quote quote = new Quote(
                "quote-505",
                "customer-505",
                new Money(50_000L, "MUR"),
                new Money(500L, "MUR"),
                new Money(0L, "MUR"),
                new Money(49_500L, "MGA"),
                Instant.now().plusSeconds(600),
                1.0d);

        service.createTransaction("idemp-005", "customer-505", quote, "beneficiary-55", "BANK");
        service.confirmReview("idemp-005", true);
        service.initiatePayin("idemp-005", "BANK");

        String payload = "{\"event\":\"payin.confirmed\",\"idempotencyKey\":\"idemp-005\"}";
        String secret = "shared-secret";
        String signature = hmacSha256(payload, secret);
        String payinRef = "PAYIN-idemp-005-BANK";

        assertThat(service.processPayinWebhook("BANK", payinRef, payload, signature, secret)).isTrue();
        assertThat(service.processPayinWebhook("BANK", payinRef, payload, signature, secret)).isFalse();
        assertThat(service.getTransaction("idemp-005").status()).isEqualTo(TransferStatus.PAYIN_CONFIRMED);
    }

    private String hmacSha256(String payload, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
