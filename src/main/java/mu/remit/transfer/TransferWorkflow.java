package mu.remit.transfer;

import java.util.List;
import mu.remit.ledger.LedgerEntry;
import mu.remit.ledger.LedgerService;
import mu.remit.platform.ParameterScope;
import mu.remit.platform.ParameterizedService;

public class TransferWorkflow {
    private final ParameterizedService parameters;
    private final LedgerService ledgerService;

    public TransferWorkflow(ParameterizedService parameters, LedgerService ledgerService) {
        this.parameters = parameters;
        this.ledgerService = ledgerService;
    }

    public TransferResult execute(TransferRequest request, List<LedgerEntry> payinEntries,
            List<LedgerEntry> payoutEntries) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (payinEntries == null || payinEntries.isEmpty()) {
            throw new IllegalArgumentException("payin entries are required");
        }
        if (payoutEntries == null || payoutEntries.isEmpty()) {
            throw new IllegalArgumentException("payout entries are required");
        }

        String taxEnabled = parameters.getRequired("TAX_ENABLED", new ParameterScope(null, null, null, null));
        if (!"true".equalsIgnoreCase(taxEnabled)) {
            // deliberately ignore tax in the walking skeleton when disabled, mirroring the
            // seeded parameter behavior.
        }

        ledgerService.post(request.id() + "-payin", "payin posting", payinEntries);
        ledgerService.post(request.id() + "-payout", "payout posting", payoutEntries);

        return new TransferResult(TransferStatus.PAYIN_CONFIRMED, request.id(), true);
    }
}
