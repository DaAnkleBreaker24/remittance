package mu.remit.ledger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LedgerService {
    private final Map<String, LedgerPostedTransaction> postedByIdempotency = new ConcurrentHashMap<>();

    public LedgerPostedTransaction post(String idempotencyKey, String description, List<LedgerEntry> entries) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotency key is required");
        }
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("entries are required");
        }

        LedgerPostedTransaction existing = postedByIdempotency.get(idempotencyKey);
        if (existing != null) {
            return existing;
        }

        validateBalancedByCurrency(entries);

        LedgerPostedTransaction posted = new LedgerPostedTransaction(idempotencyKey, description,
                new ArrayList<>(entries));
        postedByIdempotency.put(idempotencyKey, posted);
        return posted;
    }

    public LedgerIntegrityReport checkIntegrity(List<LedgerEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("entries are required");
        }

        Map<String, Long> debitsByCurrency = new HashMap<>();
        Map<String, Long> creditsByCurrency = new HashMap<>();

        for (LedgerEntry entry : entries) {
            if (entry.debit()) {
                debitsByCurrency.merge(entry.currency(), entry.amount(), Long::sum);
            } else {
                creditsByCurrency.merge(entry.currency(), entry.amount(), Long::sum);
            }
        }

        List<LedgerBalance> balances = new ArrayList<>();
        boolean balanced = true;
        for (String currency : allCurrencies(entries)) {
            long debits = debitsByCurrency.getOrDefault(currency, 0L);
            long credits = creditsByCurrency.getOrDefault(currency, 0L);
            long net = debits - credits;
            balanced = balanced && net == 0L;
            balances.add(new LedgerBalance(currency, debits, credits, net));
        }

        return new LedgerIntegrityReport(balances, balanced);
    }

    private void validateBalancedByCurrency(List<LedgerEntry> entries) {
        Map<String, Long> totalsByCurrency = new HashMap<>();
        for (LedgerEntry entry : entries) {
            String currency = entry.currency();
            long delta = entry.debit() ? entry.amount() : -entry.amount();
            totalsByCurrency.merge(currency, delta, Long::sum);
        }

        for (Map.Entry<String, Long> e : totalsByCurrency.entrySet()) {
            if (e.getValue() != 0L) {
                throw new IllegalArgumentException("ledger posting is not balanced for currency " + e.getKey());
            }
        }
    }

    private List<String> allCurrencies(List<LedgerEntry> entries) {
        return entries.stream()
                .map(LedgerEntry::currency)
                .distinct()
                .sorted()
                .toList();
    }
}
