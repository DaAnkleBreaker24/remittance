package mu.remit.reporting;

import java.util.Map;

public record LiquidityReport(long totalBalanceMinor, Map<String, Double> daysOfCover) {
    public LiquidityReport {
        if (daysOfCover == null) {
            throw new IllegalArgumentException("daysOfCover are required");
        }
    }
}
