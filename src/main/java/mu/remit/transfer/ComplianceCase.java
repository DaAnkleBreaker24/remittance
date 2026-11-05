package mu.remit.transfer;

import java.time.Instant;

public record ComplianceCase(
        String caseId,
        String reference,
        String type,
        String priority,
        String customerId,
        String assignee,
        String status,
        int slaHours,
        String decisionReason,
        Instant createdAt) {
    public ComplianceCase {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId is required");
        }
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("reference is required");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        if (priority == null || priority.isBlank()) {
            throw new IllegalArgumentException("priority is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
    }
}
