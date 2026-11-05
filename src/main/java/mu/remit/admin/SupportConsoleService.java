package mu.remit.admin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SupportConsoleService {
    private final Map<String, CustomerRecord> customers = new ConcurrentHashMap<>();
    private final Map<String, TransactionRecord> transactions = new ConcurrentHashMap<>();
    private final Map<String, List<String>> auditTrail = new ConcurrentHashMap<>();

    public void recordCustomer(String customerId, String fullName, String phone, String reference) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("full name is required");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("phone is required");
        }
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference is required");
        }

        customers.put(customerId, new CustomerRecord(customerId, fullName, phone, reference));
    }

    public void recordTransaction(String transactionId, String customerId, String reference, String status,
            String paymentMethod) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transaction id is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("payment method is required");
        }

        transactions.put(transactionId,
                new TransactionRecord(transactionId, customerId, reference, status, paymentMethod));
    }

    public List<CustomerSupportMatch> searchCustomers(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("search query is required");
        }
        String normalized = normalize(query);
        return customers.values().stream()
                .filter(customer -> customer.matches(normalized))
                .sorted(Comparator.comparing(CustomerRecord::fullName))
                .map(customer -> new CustomerSupportMatch(
                        customer.customerId(),
                        customer.fullName(),
                        maskPhone(customer.phone()),
                        customer.reference()))
                .toList();
    }

    public List<TransactionSupportMatch> searchTransactions(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("search query is required");
        }
        String normalized = normalize(query);
        return transactions.values().stream()
                .filter(transaction -> transaction.matches(normalized))
                .sorted(Comparator.comparing(TransactionRecord::transactionId))
                .map(transaction -> new TransactionSupportMatch(
                        transaction.transactionId(),
                        transaction.reference(),
                        transaction.status(),
                        transaction.paymentMethod()))
                .toList();
    }

    public SupportActionResult performAction(String agentId, String transactionId, String action, String approver,
            String secondApprover) {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agent id is required");
        }
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transaction id is required");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action is required");
        }

        TransactionRecord tx = transactions.get(transactionId);
        if (tx == null) {
            throw new IllegalArgumentException("transaction not found: " + transactionId);
        }

        boolean moneyMovement = switch (action.toLowerCase(Locale.ROOT)) {
            case "retry", "refund", "resolve" -> true;
            default -> false;
        };

        if (moneyMovement && (secondApprover == null || secondApprover.isBlank())) {
            throw new IllegalStateException("second approval required for money movement actions");
        }

        List<String> log = new ArrayList<>();
        if (moneyMovement) {
            if (approver == null || approver.isBlank()) {
                throw new IllegalStateException("second approval required for money movement actions");
            }
            log.add(agentId + ":" + action + ":" + tx.status() + ":approved-by:" + approver + ":second-approval:"
                    + secondApprover);
        } else {
            log.add(agentId + ":" + action + ":" + tx.status());
        }
        auditTrail.put(transactionId, log);
        return new SupportActionResult(transactionId, action, "APPROVED", List.copyOf(log));
    }

    public RefundDecision refund(String transactionId, String paymentMethod, String agentId, String approver,
            String secondApprover, long principalMinor, long feeMinor, long taxMinor) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transaction id is required");
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("payment method is required");
        }
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agent id is required");
        }
        if (approver == null || approver.isBlank()) {
            throw new IllegalArgumentException("approver is required");
        }
        if (secondApprover == null || secondApprover.isBlank()) {
            throw new IllegalStateException("second approval required for money movement actions");
        }

        TransactionRecord tx = transactions.get(transactionId);
        if (tx == null) {
            throw new IllegalArgumentException("transaction not found: " + transactionId);
        }

        String policy = "PRINCIPAL_AND_FEE";
        long reversal = principalMinor + feeMinor + taxMinor;
        String notification = "refund approved for " + tx.reference() + " using " + paymentMethod;
        auditTrail.computeIfAbsent(transactionId, key -> new ArrayList<>()).add(agentId + ":refund:" + policy);
        return new RefundDecision(policy, reversal, notification);
    }

    private String normalize(String value) {
        return value.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private String maskPhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "******" + digits;
        }
        return "******" + digits.substring(digits.length() - 4);
    }

    private record CustomerRecord(String customerId, String fullName, String phone, String reference) {
        boolean matches(String value) {
            return normalize(fullName).contains(value)
                    || normalize(phone).contains(value)
                    || normalize(reference).contains(value)
                    || customerId.toLowerCase(Locale.ROOT).contains(value);
        }

        private String normalize(String value) {
            return value.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
        }
    }

    private record TransactionRecord(String transactionId, String customerId, String reference, String status,
            String paymentMethod) {
        boolean matches(String value) {
            return normalize(transactionId).contains(value)
                    || normalize(reference).contains(value)
                    || normalize(status).contains(value)
                    || normalize(paymentMethod).contains(value)
                    || normalize(customerId).contains(value);
        }

        private String normalize(String value) {
            return value.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
        }
    }
}
