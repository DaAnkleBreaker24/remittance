package mu.remit.transfer;

import java.util.List;

public record Customer360(
        String customerId,
        String riskStatus,
        List<String> alerts,
        List<String> watchlistMatches,
        List<String> recentCases) {
    public Customer360 {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId is required");
        }
        if (riskStatus == null || riskStatus.isBlank()) {
            throw new IllegalArgumentException("riskStatus is required");
        }
        if (alerts == null) {
            throw new IllegalArgumentException("alerts are required");
        }
        if (watchlistMatches == null) {
            throw new IllegalArgumentException("watchlistMatches are required");
        }
        if (recentCases == null) {
            throw new IllegalArgumentException("recentCases are required");
        }
    }
}
