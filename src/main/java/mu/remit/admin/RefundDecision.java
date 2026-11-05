package mu.remit.admin;

public record RefundDecision(
        String policy,
        long ledgerReversalMinor,
        String notification) {
}
