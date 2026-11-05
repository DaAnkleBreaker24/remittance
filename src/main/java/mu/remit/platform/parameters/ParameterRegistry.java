package mu.remit.platform.parameters;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ParameterRegistry {
    private final Map<String, List<ParameterVersion>> versionsByKey = new HashMap<>();
    private final Map<String, String> cacheByKey = new ConcurrentHashMap<>();

    public ParameterVersion register(String key, String scope, String value, String createdBy) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("parameter key is required");
        }
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("parameter scope is required");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("parameter value is required");
        }
        if (createdBy == null || createdBy.isBlank()) {
            throw new IllegalArgumentException("creator is required");
        }

        List<ParameterVersion> versions = versionsByKey.computeIfAbsent(key, ignored -> new ArrayList<>());
        long nextVersion = versions.stream().mapToLong(ParameterVersion::version).max().orElse(0L) + 1;
        ParameterVersion version = new ParameterVersion(key, scope, value, createdBy, null, nextVersion, true,
                Instant.now());
        versions.add(version);
        cacheByKey.remove(key);
        return version;
    }

    public ParameterVersion approve(String key, long version, String approvedBy) {
        List<ParameterVersion> versions = versionsByKey.get(key);
        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException("parameter not found: " + key);
        }
        ParameterVersion candidate = versions.stream()
                .filter(v -> v.version() == version)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("parameter version not found: " + key + ":" + version));
        if (approvedBy == null || approvedBy.isBlank()) {
            throw new IllegalArgumentException("approver is required");
        }
        if (Objects.equals(candidate.createdBy(), approvedBy)) {
            throw new IllegalArgumentException("maker cannot approve own change");
        }

        ParameterVersion approved = new ParameterVersion(
                candidate.key(),
                candidate.scope(),
                candidate.value(),
                candidate.createdBy(),
                approvedBy,
                candidate.version(),
                true,
                candidate.effectiveFrom());
        versions.removeIf(v -> v.version() == version);
        versions.add(approved);
        cacheByKey.remove(key);
        return approved;
    }

    public String getEffective(String key, ParameterScope scope) {
        return cacheByKey.computeIfAbsent(key + "::" + scope, ignored -> findEffective(key, scope));
    }

    private String findEffective(String key, ParameterScope scope) {
        List<ParameterVersion> versions = versionsByKey.get(key);
        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException("parameter not found: " + key);
        }

        return versions.stream()
                .filter(v -> matchesScope(v.scope(), scope))
                .sorted(Comparator.comparingInt(v -> precedenceRank(v.scope(), scope)))
                .map(ParameterVersion::value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("parameter not found: " + key + " for scope " + scope));
    }

    private boolean matchesScope(String scope, ParameterScope target) {
        if (scope == null || scope.equalsIgnoreCase(ParameterScope.GLOBAL)) {
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
        return index >= 0 ? index : Integer.MAX_VALUE;
    }
}
