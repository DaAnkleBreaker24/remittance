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

        LedgerPostedTransaction posted = new LedgerPostedTransaction(idempotencyKey, description,
                new ArrayList<>(entries));
        postedByIdempotency.put(idempotencyKey, posted);
        return posted;
    }
}
