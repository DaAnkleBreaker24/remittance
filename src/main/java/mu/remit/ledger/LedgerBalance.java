package mu.remit.ledger;

public record LedgerBalance(String currency, long debits, long credits, long net) {
    public LedgerBalance {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
    }
}
