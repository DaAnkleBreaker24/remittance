package mu.remit.instruments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PaymentInstrumentService {
    private final Map<String, List<InstrumentOption>> instrumentsByCustomer = new ConcurrentHashMap<>();

    public InstrumentOption addBankAccount(String customerId, String accountName, String currency, String accountNumber,
            String type) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        if (accountName == null || accountName.isBlank()) {
            throw new IllegalArgumentException("account name is required");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("account number is required");
        }
        var instrument = new InstrumentOption(
                customerId,
                type == null || type.isBlank() ? "BANK" : type,
                "BANK",
                maskIdentifier(accountNumber),
                "bank-" + accountNumber.substring(Math.max(0, accountNumber.length() - 4)),
                "VERIFIED");
        addInstrument(customerId, instrument);
        return instrument;
    }

    public InstrumentOption addCard(String customerId, String brand, String last4, String expiry, String type) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        var instrument = new InstrumentOption(
                customerId,
                type == null || type.isBlank() ? "CARD" : type,
                brand == null || brand.isBlank() ? "visa" : brand,
                "•••• " + (last4 == null || last4.isBlank() ? "0000" : last4),
                "token-" + (last4 == null || last4.isBlank() ? "0000" : last4),
                "ACTIVE");
        addInstrument(customerId, instrument);
        return instrument;
    }

    public InstrumentOption addMaucas(String customerId, String mode, String reference, String type) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        var instrument = new InstrumentOption(
                customerId,
                type == null || type.isBlank() ? "MAUCAS" : type,
                mode == null || mode.isBlank() ? "DIRECT" : mode,
                "maucas-" + (reference == null || reference.isBlank() ? "unknown" : reference),
                reference == null || reference.isBlank() ? "maucas-ref" : reference,
                "ACTIVE");
        addInstrument(customerId, instrument);
        return instrument;
    }

    public List<String> availableTypes(String customerId, String corridor, String kycLevel) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customer id is required");
        }
        var list = new ArrayList<String>();
        list.add("BANK");
        list.add("CARD");
        list.add("MAUCAS");
        if (corridor != null && corridor.equalsIgnoreCase("MGA") && (kycLevel == null || kycLevel.isBlank())) {
            list.remove("CARD");
        }
        return list;
    }

    private void addInstrument(String customerId, InstrumentOption instrument) {
        instrumentsByCustomer.computeIfAbsent(customerId, key -> new ArrayList<>()).add(instrument);
    }

    private String maskIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "****";
        }
        if (value.length() <= 4) {
            return "****" + value;
        }
        return "******" + value.substring(value.length() - 4);
    }
}
