package mu.remit.ledger;

public record LedgerEntry(Account account, long amount, String currency, boolean debit) {
    public LedgerEntry {
        if (account == null) {
            throw new IllegalArgumentException("account is required");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
    }

    public String direction() {
        return debit ? "DEBIT" : "CREDIT";
    }
}
