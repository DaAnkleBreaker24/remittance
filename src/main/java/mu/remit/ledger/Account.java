package mu.remit.ledger;

public record Account(String code, String name, AccountType type, String currency) {
    public Account {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("account code is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("account name is required");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("account type is required");
        }
    }
}
