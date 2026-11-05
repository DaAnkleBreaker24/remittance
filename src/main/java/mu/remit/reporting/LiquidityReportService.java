package mu.remit.reporting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LiquidityReportService {

    public LiquidityReport generate(List<LiquidityBalance> balances) {
        if (balances == null) {
            throw new IllegalArgumentException("balances are required");
        }

        long totalBalance = balances.stream().mapToLong(LiquidityBalance::balanceMinor).sum();
        Map<String, Double> daysOfCover = new LinkedHashMap<>();
        for (LiquidityBalance balance : balances) {
            daysOfCover.put(balance.currency(), balance.daysOfCover());
        }
        return new LiquidityReport(totalBalance, daysOfCover);
    }
}
