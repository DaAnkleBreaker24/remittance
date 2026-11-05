package mu.remit.payout;

import java.util.List;

public record Corridor(String code, String sendCurrency, String receiveCurrency, List<PayoutMethod> methods) {
    public Corridor {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("corridor code is required");
        }
        if (sendCurrency == null || sendCurrency.isBlank()) {
            throw new IllegalArgumentException("send currency is required");
        }
        if (receiveCurrency == null || receiveCurrency.isBlank()) {
            throw new IllegalArgumentException("receive currency is required");
        }
        if (methods == null || methods.isEmpty()) {
            throw new IllegalArgumentException("at least one payout method is required");
        }
    }
}
