package mu.remit.payout;

import java.util.Comparator;
import java.util.List;

public class CorridorRouter {
    private final List<Corridor> corridors;

    public CorridorRouter(List<Corridor> corridors) {
        this.corridors = corridors == null ? List.of() : List.copyOf(corridors);
    }

    public PayoutRoute route(String corridorCode, String channel, String sendCurrency, String receiveCurrency) {
        if (corridorCode == null || corridorCode.isBlank()) {
            throw new IllegalArgumentException("corridor code is required");
        }
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("channel is required");
        }
        if (sendCurrency == null || sendCurrency.isBlank()) {
            throw new IllegalArgumentException("send currency is required");
        }
        if (receiveCurrency == null || receiveCurrency.isBlank()) {
            throw new IllegalArgumentException("receive currency is required");
        }

        Corridor corridor = corridors.stream()
                .filter(c -> c.code().equalsIgnoreCase(corridorCode))
                .filter(c -> c.sendCurrency().equalsIgnoreCase(sendCurrency))
                .filter(c -> c.receiveCurrency().equalsIgnoreCase(receiveCurrency))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no corridor route for " + corridorCode));

        PayoutMethod method = corridor.methods().stream()
                .filter(m -> m.channel().equalsIgnoreCase(channel))
                .sorted(Comparator.comparingInt(m -> "MOBILE".equalsIgnoreCase(m.code()) ? 0 : 1))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no payout method for corridor " + corridorCode));

        return new PayoutRoute(corridor.code(), method, corridor.receiveCurrency());
    }
}
