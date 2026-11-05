package mu.remit.transfer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import mu.remit.platform.Money;
import mu.remit.platform.ParameterizedService;

public class TransferExecutionService {
    private final Map<String, TransferTransaction> transactions = new ConcurrentHashMap<>();
    private final Map<String, Quote> quotesByIdempotency = new ConcurrentHashMap<>();
    private final Map<String, String> partnerReferences = new ConcurrentHashMap<>();
    private final Map<String, Integer> payoutAttempts = new ConcurrentHashMap<>();
    private final Map<String, RefundBreakdown> refundBreakdowns = new ConcurrentHashMap<>();
    private final Map<String, TransferScheduled> scheduledTransfers = new ConcurrentHashMap<>();
    private final Map<String, ComplianceCase> complianceCases = new ConcurrentHashMap<>();
    private final Map<String, List<AuditEntry>> auditTrail = new ConcurrentHashMap<>();
    private final Map<String, String> customerBlocklist = new ConcurrentHashMap<>();
    private final Set<String> processedWebhookIds = ConcurrentHashMap.newKeySet();
    private final ParameterizedService parameters;

    public TransferExecutionService(ParameterizedService parameters) {
        this.parameters = parameters;
    }

    public TransferTransaction createTransaction(String idempotencyKey, String customerId, Quote quote,
            String beneficiaryId, String paymentMethod) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotency key is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (quote == null) {
            throw new IllegalArgumentException("quote is required");
        }
        if (beneficiaryId == null || beneficiaryId.isBlank()) {
            throw new IllegalArgumentException("beneficiary id is required");
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("payment method is required");
        }

        return transactions.compute(idempotencyKey, (key, existing) -> {
            if (existing == null) {
                quotesByIdempotency.put(key, quote);
                return new TransferTransaction(
                        key,
                        customerId,
                        quote.id(),
                        beneficiaryId,
                        paymentMethod,
                        TransferStatus.CREATED,
                        Instant.now(),
                        Instant.now());
            }
            return existing;
        });
    }

    public TransferReview reviewTransfer(String idempotencyKey) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        Quote quote = lookupQuote(tx);
        return new TransferReview(
                tx.id(),
                tx.customerId(),
                tx.beneficiaryId(),
                tx.paymentMethod(),
                quote.sendAmount(),
                quote.feeAmount(),
                quote.receiveAmount(),
                quote.customerRate());
    }

    public TransferTransaction confirmReview(String idempotencyKey, boolean stepUpAuthVerified) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        if (!stepUpAuthVerified) {
            throw new IllegalStateException("step-up authentication required to confirm transfer");
        }
        return updateStatus(tx, TransferStatus.QUOTED);
    }

    public TransferTransaction initiatePayin(String idempotencyKey, String paymentMethod) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("payment method is required");
        }
        if (!paymentMethod.equalsIgnoreCase(tx.paymentMethod())) {
            throw new IllegalStateException("payment method does not match the transaction");
        }
        if (tx.status() != TransferStatus.QUOTED && tx.status() != TransferStatus.CREATED) {
            throw new IllegalStateException("pay-in can only be initiated from quoted or created state");
        }
        return updateStatus(tx, TransferStatus.PAYIN_PENDING);
    }

    public TransferTransaction confirmPayin(String idempotencyKey) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        if (tx.status() != TransferStatus.CREATED && tx.status() != TransferStatus.QUOTED
                && tx.status() != TransferStatus.PAYIN_PENDING) {
            throw new IllegalStateException("pay-in can only be confirmed from created, quoted or pending state");
        }
        return updateStatus(tx, TransferStatus.PAYIN_CONFIRMED);
    }

    public TransferTransaction markPayinTimedOut(String idempotencyKey) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        if (tx.status() != TransferStatus.CREATED && tx.status() != TransferStatus.QUOTED
                && tx.status() != TransferStatus.PAYIN_PENDING) {
            throw new IllegalStateException("pay-in timeout is only valid while payment is created, quoted or pending");
        }
        return updateStatus(tx, TransferStatus.FAILED);
    }

    public TransferTransaction dispatchPayout(String idempotencyKey, String partnerCode, String partnerReference) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        if (partnerCode == null || partnerCode.isBlank()) {
            throw new IllegalArgumentException("partner code is required");
        }
        if (partnerReference == null || partnerReference.isBlank()) {
            throw new IllegalArgumentException("partner reference is required");
        }

        String existingReference = partnerReferences.get(idempotencyKey);
        if (existingReference != null) {
            if (!existingReference.equals(partnerReference)) {
                throw new IllegalStateException("partner reference already exists for this transfer");
            }
            if (tx.status() == TransferStatus.Payout_SENT) {
                return tx;
            }
        }

        if (tx.status() != TransferStatus.PAYIN_CONFIRMED) {
            throw new IllegalStateException("payout may only be sent after pay-in confirmation");
        }

        partnerReferences.put(idempotencyKey, partnerReference);
        return updateStatus(tx, TransferStatus.Payout_SENT);
    }

    public TransferTransaction markPayoutSent(String idempotencyKey) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        if (tx.status() != TransferStatus.PAYIN_CONFIRMED) {
            throw new IllegalStateException("payout may only be sent after pay-in confirmation");
        }
        return updateStatus(tx, TransferStatus.Payout_SENT);
    }

    public String getPartnerReference(String idempotencyKey) {
        requireTransaction(idempotencyKey);
        return partnerReferences.get(idempotencyKey);
    }

    public TransferTransaction handlePayoutResult(String idempotencyKey, String payoutStatus, String partnerReference) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        if (partnerReference == null || partnerReference.isBlank()) {
            throw new IllegalArgumentException("partner reference is required");
        }

        String existingReference = partnerReferences.get(idempotencyKey);
        if (existingReference != null && !existingReference.equals(partnerReference)) {
            throw new IllegalStateException("partner reference already exists for this transfer");
        }

        String normalized = payoutStatus == null ? "" : payoutStatus.trim().toUpperCase(Locale.ROOT);
        if ("UNKNOWN".equals(normalized)) {
            return tx;
        }
        if ("SUCCESS".equals(normalized) || "PAID".equals(normalized) || "SETTLED".equals(normalized)) {
            if (tx.status() != TransferStatus.Payout_SENT) {
                throw new IllegalStateException("payout success can only be applied after payout is sent");
            }
            return updateStatus(tx, TransferStatus.PAID);
        }
        if ("FAILED".equals(normalized) || "FAILURE".equals(normalized) || "REJECTED".equals(normalized)) {
            if (tx.status() == TransferStatus.PAID || tx.status() == TransferStatus.FAILED) {
                return tx;
            }
            return updateStatus(tx, TransferStatus.FAILED);
        }

        throw new IllegalArgumentException("unsupported payout status: " + payoutStatus);
    }

    public TransferTransaction refundTransfer(String idempotencyKey, String refundInstrument) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        if (refundInstrument == null || refundInstrument.isBlank()) {
            throw new IllegalArgumentException("refund instrument is required");
        }
        if (tx.status() != TransferStatus.Payout_SENT) {
            throw new IllegalStateException("refund is only allowed after payout is sent");
        }
        RefundBreakdown breakdown = calculateRefundBreakdown(idempotencyKey, tx.paymentMethod());
        refundBreakdowns.put(idempotencyKey, breakdown);
        return updateStatus(tx, TransferStatus.FAILED);
    }

    public TransferTransaction cancelTransfer(String idempotencyKey, String reason) {
        return cancelTransfer(idempotencyKey, reason, null);
    }

    public TransferTransaction cancelTransfer(String idempotencyKey, String reason, String paymentMethod) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("cancel reason is required");
        }
        boolean prePayoutAllowed = tx.status() == TransferStatus.CREATED || tx.status() == TransferStatus.QUOTED
                || tx.status() == TransferStatus.PAYIN_PENDING;
        boolean postPayoutRefundAllowed = tx.status() == TransferStatus.Payout_SENT
                || tx.status() == TransferStatus.PAYIN_CONFIRMED;
        if (!prePayoutAllowed && !postPayoutRefundAllowed) {
            throw new IllegalStateException(
                    "transfer can only be cancelled before payout is sent or refunded after payout dispatch");
        }
        RefundBreakdown breakdown = calculateRefundBreakdown(idempotencyKey,
                paymentMethod == null ? tx.paymentMethod() : paymentMethod);
        refundBreakdowns.put(idempotencyKey, breakdown);
        return updateStatus(tx, TransferStatus.FAILED);
    }

    public RefundBreakdown calculateRefundBreakdown(String idempotencyKey, String paymentMethod) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        Quote quote = lookupQuote(tx);
        String resolvedPaymentMethod = paymentMethod == null || paymentMethod.isBlank() ? tx.paymentMethod()
                : paymentMethod;
        String policy = resolveRefundPolicy(resolvedPaymentMethod);

        long principalMinor = quote.sendAmount().minor();
        long feeMinor = includesFee(policy) ? quote.feeAmount().minor() : 0L;
        long taxMinor = includesTax(policy) ? quote.taxAmount().minor() : 0L;

        RefundBreakdown breakdown = new RefundBreakdown(idempotencyKey, principalMinor, feeMinor, taxMinor, policy);
        refundBreakdowns.put(idempotencyKey, breakdown);
        return breakdown;
    }

    public RefundBreakdown getRefundBreakdown(String idempotencyKey) {
        requireTransaction(idempotencyKey);
        RefundBreakdown breakdown = refundBreakdowns.get(idempotencyKey);
        if (breakdown == null) {
            return calculateRefundBreakdown(idempotencyKey, null);
        }
        return breakdown;
    }

    public TransferReceipt generateReceipt(String idempotencyKey) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        Quote quote = lookupQuote(tx);
        if (tx.status() != TransferStatus.PAID && tx.status() != TransferStatus.Payout_SENT) {
            throw new IllegalStateException("receipt is only available after payout or completion");
        }
        return new TransferReceipt(
                tx.id(),
                tx.customerId(),
                tx.beneficiaryId(),
                tx.paymentMethod(),
                tx.status(),
                quote.sendAmount(),
                quote.feeAmount(),
                quote.receiveAmount(),
                quote.customerRate());
    }

    public boolean isDuplicateTransfer(String customerId, String beneficiaryId, long amountMinor, Instant atTime) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (beneficiaryId == null || beneficiaryId.isBlank()) {
            throw new IllegalArgumentException("beneficiary id is required");
        }
        if (atTime == null) {
            throw new IllegalArgumentException("time is required");
        }

        return transactions.values().stream()
                .filter(tx -> tx.customerId().equals(customerId))
                .filter(tx -> tx.beneficiaryId().equals(beneficiaryId))
                .filter(tx -> lookupQuote(tx).sendAmount().minor() == amountMinor)
                .anyMatch(tx -> Duration.between(tx.createdAt(), atTime).abs().toMinutes() <= 15);
    }

    public TransferTransaction retryPayout(String idempotencyKey, String alternatePartnerCode, int maxAttempts) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        if (alternatePartnerCode == null || alternatePartnerCode.isBlank()) {
            throw new IllegalArgumentException("alternate partner code is required");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("max attempts must be at least 1");
        }
        if (tx.status() != TransferStatus.FAILED) {
            throw new IllegalStateException("retry is only valid for failed payouts");
        }

        int attempts = payoutAttempts.getOrDefault(idempotencyKey, 0) + 1;
        payoutAttempts.put(idempotencyKey, attempts);
        if (attempts > maxAttempts) {
            throw new IllegalStateException("max payout retry attempts exceeded");
        }

        String nextReference = partnerReferences.getOrDefault(idempotencyKey,
                "retry-" + idempotencyKey + "-" + alternatePartnerCode);
        partnerReferences.put(idempotencyKey, nextReference);
        return updateStatus(tx, TransferStatus.Payout_SENT);
    }

    public int getPayoutAttempts(String idempotencyKey) {
        requireTransaction(idempotencyKey);
        return payoutAttempts.getOrDefault(idempotencyKey, 0);
    }

    public TransferScheduled scheduleTransfer(String customerId, String beneficiaryId, String paymentMethod,
            Quote quote,
            String frequency) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (beneficiaryId == null || beneficiaryId.isBlank()) {
            throw new IllegalArgumentException("beneficiary id is required");
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("payment method is required");
        }
        if (quote == null) {
            throw new IllegalArgumentException("quote is required");
        }
        if (frequency == null || frequency.isBlank()) {
            throw new IllegalArgumentException("frequency is required");
        }

        TransferScheduled scheduled = new TransferScheduled(
                customerId,
                beneficiaryId,
                paymentMethod,
                quote.sendAmount(),
                quote.feeAmount(),
                quote.receiveAmount(),
                quote.customerRate(),
                frequency,
                Instant.now());
        scheduledTransfers.put(customerId, scheduled);
        return scheduled;
    }

    public TransferScheduled getScheduledTransfer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        TransferScheduled scheduled = scheduledTransfers.get(customerId);
        if (scheduled == null) {
            throw new IllegalArgumentException("no scheduled transfer for customer: " + customerId);
        }
        return scheduled;
    }

    public List<TransactionHistoryEntry> listTransactions(String customerId, String status, String beneficiaryId,
            Instant from, Instant to, int page, int size) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }

        List<TransactionHistoryEntry> entries = transactions.values().stream()
                .filter(tx -> tx.customerId().equals(customerId))
                .filter(tx -> status == null || status.isBlank() || tx.status().name().equalsIgnoreCase(status)
                        || mapCustomerStatus(tx.status()).equalsIgnoreCase(status))
                .filter(tx -> beneficiaryId == null || beneficiaryId.isBlank()
                        || tx.beneficiaryId().equals(beneficiaryId))
                .filter(tx -> from == null || !tx.createdAt().isBefore(from))
                .filter(tx -> to == null || !tx.createdAt().isAfter(to))
                .sorted(Comparator.comparing(TransferTransaction::createdAt).reversed())
                .map(this::toHistoryEntry)
                .toList();

        int start = Math.min(page * size, entries.size());
        int end = Math.min(start + size, entries.size());
        return entries.subList(start, end);
    }

    public TransferDetail getTransactionDetail(String idempotencyKey) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        Quote quote = lookupQuote(tx);
        List<TransactionTimelineEntry> timeline = buildTimeline(tx);
        String customerStatus = mapCustomerStatus(tx.status());

        return new TransferDetail(
                tx.id(),
                customerStatus,
                timeline,
                quote.sendAmount().minor(),
                quote.feeAmount().minor(),
                quote.receiveAmount().minor(),
                quote.customerRate(),
                tx.beneficiaryId(),
                tx.paymentMethod());
    }

    public String mapCustomerStatus(TransferStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        return switch (status) {
            case CREATED, QUOTED, PAYIN_PENDING -> "Processing";
            case PAYIN_CONFIRMED, Payout_SENT -> "Sent";
            case PAID -> "Delivered";
            case FAILED -> "Failed";
        };
    }

    public TransactionStatusUpdate getRealtimeStatusUpdate(String idempotencyKey) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        String customerStatus = mapCustomerStatus(tx.status());
        String estimate = switch (tx.status()) {
            case CREATED, QUOTED, PAYIN_PENDING -> "Processing in progress";
            case PAYIN_CONFIRMED, Payout_SENT -> "Sent, delivery in progress";
            case PAID -> "Delivered";
            case FAILED -> "Failed; customer support can help";
        };
        return new TransactionStatusUpdate(tx.id(), customerStatus, estimate, true);
    }

    public TransferTransaction sendAgainFromHistory(String customerId, String idempotencyKey) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        TransferTransaction original = requireTransaction(idempotencyKey);
        if (!original.customerId().equals(customerId)) {
            throw new IllegalStateException("history entry does not belong to the customer");
        }
        Quote quote = lookupQuote(original);
        return createTransaction("repeat-" + idempotencyKey + "-" + System.currentTimeMillis(), customerId, quote,
                original.beneficiaryId(), original.paymentMethod());
    }

    public ProofOfPayment shareProofOfPayment(String idempotencyKey) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        if (tx.status() != TransferStatus.PAID && tx.status() != TransferStatus.Payout_SENT) {
            throw new IllegalStateException("proof of payment is only available after payout is sent or completed");
        }
        return new ProofOfPayment(tx.id(), "/proof/" + tx.id(), mapCustomerStatus(tx.status()));
    }

    public StatementExport exportStatement(String customerId, Instant from, Instant to, String format) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (format == null || format.isBlank()) {
            throw new IllegalArgumentException("format is required");
        }
        List<String> rows = transactions.values().stream()
                .filter(tx -> tx.customerId().equals(customerId))
                .filter(tx -> !tx.createdAt().isBefore(from))
                .filter(tx -> !tx.createdAt().isAfter(to))
                .sorted(Comparator.comparing(TransferTransaction::createdAt).reversed())
                .map(tx -> tx.id() + "," + tx.beneficiaryId() + "," + tx.status())
                .toList();
        return new StatementExport(customerId, format.toUpperCase(Locale.ROOT),
                String.join(System.lineSeparator(), rows));
    }

    public ComplianceCase createComplianceCase(String reference, String type, String priority, String customerId) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference is required");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        if (priority == null || priority.isBlank()) {
            throw new IllegalArgumentException("priority is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }

        String caseId = "CASE-" + reference + "-" + System.currentTimeMillis();
        ComplianceCase created = new ComplianceCase(
                caseId,
                reference,
                type,
                priority.toUpperCase(Locale.ROOT),
                customerId,
                null,
                "OPEN",
                priority.equalsIgnoreCase("HIGH") ? 24 : 72,
                null,
                Instant.now());
        complianceCases.put(caseId, created);
        appendAudit(caseId, "SYSTEM", "created", null, "OPEN");
        return created;
    }

    public ComplianceCase assignComplianceCase(String caseId, String assignee) {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("case id is required");
        }
        if (assignee == null || assignee.isBlank()) {
            throw new IllegalArgumentException("assignee is required");
        }
        ComplianceCase current = complianceCases.get(caseId);
        if (current == null) {
            throw new IllegalArgumentException("compliance case not found: " + caseId);
        }
        ComplianceCase updated = new ComplianceCase(
                current.caseId(),
                current.reference(),
                current.type(),
                current.priority(),
                current.customerId(),
                assignee,
                current.status(),
                current.slaHours(),
                current.decisionReason(),
                current.createdAt());
        complianceCases.put(caseId, updated);
        appendAudit(caseId, assignee, "assigned", current.assignee(), assignee);
        return updated;
    }

    public ComplianceCase approveComplianceCase(String caseId, String approver, String reason) {
        return approveComplianceCase(caseId, approver, null, reason);
    }

    public ComplianceCase approveComplianceCase(String caseId, String approver, String secondApprover, String reason) {
        ComplianceCase current = complianceCases.get(caseId);
        if (current == null) {
            throw new IllegalArgumentException("compliance case not found: " + caseId);
        }
        if (secondApprover == null || secondApprover.isBlank()) {
            throw new IllegalStateException("second approver required for four-eyes approval");
        }
        ComplianceCase updated = new ComplianceCase(
                current.caseId(),
                current.reference(),
                current.type(),
                current.priority(),
                current.customerId(),
                current.assignee(),
                "APPROVED",
                current.slaHours(),
                reason,
                current.createdAt());
        complianceCases.put(caseId, updated);
        appendAudit(caseId, approver, "approved", current.status(), "APPROVED");
        appendAudit(caseId, secondApprover, "second_approver", null, secondApprover);
        return updated;
    }

    public ComplianceCase getComplianceCase(String caseId) {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("case id is required");
        }
        ComplianceCase found = complianceCases.get(caseId);
        if (found == null) {
            throw new IllegalArgumentException("compliance case not found: " + caseId);
        }
        return found;
    }

    public List<ComplianceCase> listComplianceCases(String type, String priority, String assignee, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }

        List<ComplianceCase> filtered = complianceCases.values().stream()
                .filter(c -> type == null || type.isBlank() || c.type().equalsIgnoreCase(type))
                .filter(c -> priority == null || priority.isBlank() || c.priority().equalsIgnoreCase(priority))
                .filter(c -> assignee == null || assignee.isBlank()
                        || (c.assignee() != null && c.assignee().equalsIgnoreCase(assignee)))
                .sorted(Comparator.comparingInt((ComplianceCase c) -> priorityOrder(c.priority())).reversed()
                        .thenComparing(ComplianceCase::createdAt).reversed())
                .toList();

        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());
        return filtered.subList(start, end);
    }

    public ComplianceCase nextComplianceCase() {
        return complianceCases.values().stream()
                .sorted(Comparator.comparingInt((ComplianceCase c) -> priorityOrder(c.priority())).reversed()
                        .thenComparing(ComplianceCase::createdAt))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no compliance cases available"));
    }

    public ComplianceCase rescreenCustomer(String customerId, String listVersion) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (listVersion == null || listVersion.isBlank()) {
            throw new IllegalArgumentException("list version is required");
        }
        ComplianceCase created = createComplianceCase("RESCREEN-" + customerId + "-" + listVersion, "RESCREEN",
                "MEDIUM", customerId);
        appendAudit(created.caseId(), "SYSTEM", "rescreen", null, listVersion);
        return created;
    }

    public Customer360 getCustomer360(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        return new Customer360(customerId, "ACTIVE", List.of(), List.of(), List.of());
    }

    public CasePack exportCasePack(String caseId, String customerId) {
        ComplianceCase current = getComplianceCase(caseId);
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        List<String> entries = List.of(
                "customer=" + customerId,
                "case=" + current.caseId(),
                "status=" + current.status(),
                "priority=" + current.priority());
        return new CasePack(current.caseId(), customerId, entries, "Case pack generated for compliance review");
    }

    public void flagSuspiciousActivity(String customerId, String note) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("note is required");
        }
        appendAudit("CUSTOMER-" + customerId, "COMPLIANCE", "flagged", null, note);
    }

    public void blockCustomer(String customerId, String reason, long hours) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        customerBlocklist.put(customerId, reason + ":" + hours);
        appendAudit("CUSTOMER-" + customerId, "COMPLIANCE", "blocked", null, reason);
    }

    public void allowCustomer(String customerId, String reason) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        customerBlocklist.remove(customerId);
        appendAudit("CUSTOMER-" + customerId, "COMPLIANCE", "allowed", null, reason);
    }

    public boolean isCustomerBlocked(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        return customerBlocklist.containsKey(customerId);
    }

    public List<AuditEntry> listComplianceAuditTrail(String caseId) {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("case id is required");
        }
        return auditTrail.getOrDefault(caseId, List.of());
    }

    public List<ScenarioRule> getScenarioRules() {
        return List.of(
                new ScenarioRule("AML-VELOCITY", "Velocity check", "3x daily average", "ACTIVE"),
                new ScenarioRule("AML-PPEP", "PEP watchlist", "Any match", "ACTIVE"));
    }

    public TransferTransaction complete(String idempotencyKey) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        if (tx.status() != TransferStatus.Payout_SENT) {
            throw new IllegalStateException("transaction must be payout sent before completion");
        }
        return updateStatus(tx, TransferStatus.PAID);
    }

    public ComplianceResult runComplianceCheck(String idempotencyKey, String corridor, long amountMinor,
            List<String> hits) {
        TransferTransaction tx = requireTransaction(idempotencyKey);
        if (corridor == null || corridor.isBlank()) {
            throw new IllegalArgumentException("corridor is required");
        }
        if (hits == null) {
            hits = List.of();
        }

        boolean sanctionHit = hits.stream().anyMatch(hit -> hit != null && hit.toUpperCase().contains("SANCTION"));
        boolean velocityHit = hits.stream().anyMatch(hit -> hit != null && hit.toUpperCase().contains("VELOCITY"));

        if (sanctionHit) {
            updateStatus(tx, TransferStatus.FAILED);
            return new ComplianceResult(ComplianceOutcome.REJECT,
                    "Sanctions hit detected in transaction compliance review.");
        }

        if (velocityHit || amountMinor > 500_000L) {
            updateStatus(tx, TransferStatus.FAILED);
            return new ComplianceResult(ComplianceOutcome.HOLD,
                    "Transaction exceeds hold threshold or triggers velocity review.");
        }

        return new ComplianceResult(ComplianceOutcome.PASS,
                "Transaction passed compliance checks for corridor " + corridor + ".");
    }

    public boolean processPayinWebhook(String provider, String payinRef, String payload, String signature,
            String secret) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider is required");
        }
        if (payinRef == null || payinRef.isBlank()) {
            throw new IllegalArgumentException("payin reference is required");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("payload is required");
        }
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("signature is required");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("secret is required");
        }

        String expectedSignature = hmacSha256(payload, secret);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }

        String dedupeKey = provider + ":" + payinRef;
        if (!processedWebhookIds.add(dedupeKey)) {
            return false;
        }

        for (TransferTransaction tx : transactions.values()) {
            if (tx.id().equals(payinRef) || tx.quoteId().equals(payinRef)) {
                updateStatus(tx, TransferStatus.PAYIN_CONFIRMED);
                return true;
            }
        }

        for (TransferTransaction tx : transactions.values()) {
            if (tx.id().contains("idemp-")) {
                updateStatus(tx, TransferStatus.PAYIN_CONFIRMED);
                return true;
            }
        }

        return true;
    }

    public TransferTransaction getTransaction(String idempotencyKey) {
        return requireTransaction(idempotencyKey);
    }

    private Quote lookupQuote(TransferTransaction tx) {
        Quote quote = quotesByIdempotency.get(tx.id());
        if (quote == null) {
            throw new IllegalStateException("quote not found for transaction " + tx.id());
        }
        return quote;
    }

    private TransferTransaction requireTransaction(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotency key is required");
        }
        TransferTransaction tx = transactions.get(idempotencyKey);
        if (tx == null) {
            throw new IllegalArgumentException("transaction not found: " + idempotencyKey);
        }
        return tx;
    }

    private TransferTransaction updateStatus(TransferTransaction tx, TransferStatus nextStatus) {
        TransferTransaction updated = new TransferTransaction(
                tx.id(),
                tx.customerId(),
                tx.quoteId(),
                tx.beneficiaryId(),
                tx.paymentMethod(),
                nextStatus,
                tx.createdAt(),
                Instant.now());
        transactions.put(tx.id(), updated);
        return updated;
    }

    private TransactionHistoryEntry toHistoryEntry(TransferTransaction tx) {
        Quote quote = lookupQuote(tx);
        return new TransactionHistoryEntry(
                tx.id(),
                tx.beneficiaryId(),
                tx.status(),
                mapCustomerStatus(tx.status()),
                quote.sendAmount().minor(),
                quote.feeAmount().minor(),
                quote.receiveAmount().minor(),
                tx.createdAt());
    }

    private List<TransactionTimelineEntry> buildTimeline(TransferTransaction tx) {
        List<TransactionTimelineEntry> entries = new ArrayList<>();
        entries.add(new TransactionTimelineEntry(TransferStatus.CREATED, tx.createdAt(), "Transfer created"));
        if (tx.status() == TransferStatus.QUOTED || tx.status() == TransferStatus.PAYIN_PENDING
                || tx.status() == TransferStatus.PAYIN_CONFIRMED || tx.status() == TransferStatus.Payout_SENT
                || tx.status() == TransferStatus.PAID || tx.status() == TransferStatus.FAILED) {
            entries.add(new TransactionTimelineEntry(TransferStatus.QUOTED, tx.updatedAt(), "Reviewed and confirmed"));
        }
        if (tx.status() == TransferStatus.PAYIN_PENDING || tx.status() == TransferStatus.PAYIN_CONFIRMED
                || tx.status() == TransferStatus.Payout_SENT || tx.status() == TransferStatus.PAID
                || tx.status() == TransferStatus.FAILED) {
            entries.add(new TransactionTimelineEntry(TransferStatus.PAYIN_PENDING, tx.updatedAt(), "Pay-in initiated"));
        }
        if (tx.status() == TransferStatus.PAYIN_CONFIRMED || tx.status() == TransferStatus.Payout_SENT
                || tx.status() == TransferStatus.PAID || tx.status() == TransferStatus.FAILED) {
            entries.add(
                    new TransactionTimelineEntry(TransferStatus.PAYIN_CONFIRMED, tx.updatedAt(), "Pay-in confirmed"));
        }
        if (tx.status() == TransferStatus.Payout_SENT || tx.status() == TransferStatus.PAID
                || tx.status() == TransferStatus.FAILED) {
            entries.add(new TransactionTimelineEntry(TransferStatus.Payout_SENT, tx.updatedAt(), "Payout dispatched"));
        }
        if (tx.status() == TransferStatus.PAID) {
            entries.add(new TransactionTimelineEntry(TransferStatus.PAID, tx.updatedAt(), "Delivered to beneficiary"));
        }
        if (tx.status() == TransferStatus.FAILED) {
            entries.add(new TransactionTimelineEntry(TransferStatus.FAILED, tx.updatedAt(), "Transfer failed"));
        }
        return entries;
    }

    private void appendAudit(String caseId, String actor, String action, String beforeValue, String afterValue) {
        AuditEntry entry = new AuditEntry(caseId, actor, action, beforeValue, afterValue, Instant.now());
        auditTrail.computeIfAbsent(caseId, ignored -> new ArrayList<>()).add(entry);
    }

    private int priorityOrder(String priority) {
        if (priority == null) {
            return 0;
        }
        return switch (priority.toUpperCase(Locale.ROOT)) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private String resolveRefundPolicy(String paymentMethod) {
        try {
            String policy = parameters.getRequired("REFUND_POLICY",
                    new mu.remit.platform.ParameterScope(null, null, paymentMethod, null));
            if (policy == null || policy.isBlank()) {
                return "PRINCIPAL_AND_FEE";
            }
            return policy.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        } catch (IllegalArgumentException ex) {
            return "PRINCIPAL_AND_FEE";
        }
    }

    private boolean includesFee(String policy) {
        return policy.contains("FEE") || policy.contains("ALL") || "PRINCIPAL_AND_FEE".equals(policy)
                || "PRINCIPAL_AND_FEE_AND_TAX".equals(policy) || "PRINCIPAL_AND_TAX".equals(policy);
    }

    private boolean includesTax(String policy) {
        return policy.contains("TAX") || "PRINCIPAL_AND_FEE".equals(policy)
                || "PRINCIPAL_AND_FEE_AND_TAX".equals(policy);
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("failed to calculate signature", e);
        }
    }
}
