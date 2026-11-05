package mu.remit.platform.parameters;

import java.time.Instant;

public record ParameterVersion(
        String key,
        String scope,
        String value,
        String createdBy,
        String approvedBy,
        long version,
        boolean active,
        Instant effectiveFrom,
        String reason) {
}
