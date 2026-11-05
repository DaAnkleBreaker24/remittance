package mu.remit.platform.parameters;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ParameterizedService {
    private final Map<String, List<ParameterValue>> byKey;

    public ParameterizedService(List<ParameterValue> parameters) {
        this.byKey = parameters.stream()
                .collect(Collectors.groupingBy(ParameterValue::key));
    }

    public String getRequired(String key, ParameterScope scope) {
        List<ParameterValue> values = byKey.getOrDefault(key, List.of());
        if (values.isEmpty()) {
            throw new IllegalArgumentException("parameter not found: " + key);
        }

        return values.stream()
                .filter(v -> matchesScope(v.scope(), scope))
                .sorted(Comparator.comparingInt(v -> precedenceRank(v.scope(), scope)))
                .map(ParameterValue::value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("parameter not found: " + key));
    }

    private boolean matchesScope(String scope, ParameterScope target) {
        if (Objects.equals(scope, ParameterScope.GLOBAL)) {
            return true;
        }
        if (scope.startsWith("SEGMENT:")) {
            return Objects.equals(target.segment(), scope.substring("SEGMENT:".length()));
        }
        if (scope.startsWith("PAYMENT_TYPE:")) {
            return Objects.equals(target.paymentType(), scope.substring("PAYMENT_TYPE:".length()));
        }
        if (scope.startsWith("CHANNEL:")) {
            return Objects.equals(target.channel(), scope.substring("CHANNEL:".length()));
        }
        if (scope.startsWith("CORRIDOR:")) {
            return Objects.equals(target.corridor(), scope.substring("CORRIDOR:".length()));
        }
        return false;
    }

    private int precedenceRank(String scope, ParameterScope target) {
        List<String> order = target.precedenceOrder();
        int index = order.indexOf(scope);
        return index >= 0 ? index : order.size();
    }
}
