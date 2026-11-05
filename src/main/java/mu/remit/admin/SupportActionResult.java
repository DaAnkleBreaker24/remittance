package mu.remit.admin;

import java.util.List;

public record SupportActionResult(
        String transactionId,
        String action,
        String status,
        List<String> auditTrail) {
}
