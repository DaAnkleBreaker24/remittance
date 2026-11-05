package mu.remit.ledger;

import java.util.List;

public record LedgerIntegrityReport(List<LedgerBalance> balances, boolean balanced) {
    public LedgerIntegrityReport {
        if (balances == null) {
            throw new IllegalArgumentException("balances are required");
        }
    }
}
