package mu.remit.instruments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentInstrumentServiceTest {

    @Test
    void resolvesAvailableInstrumentTypesAndStoresMaskedMetadata() {
        PaymentInstrumentService service = new PaymentInstrumentService();

        InstrumentOption bank = service.addBankAccount(
                "customer-500",
                "John Doe",
                "MUR",
                "1234567890",
                "BANK");

        InstrumentOption card = service.addCard(
                "customer-500",
                "visa",
                "4242",
                "12/29",
                "CARD");

        InstrumentOption maucas = service.addMaucas(
                "customer-500",
                "DIRECT",
                "txn-1001",
                "MAUCAS");

        List<String> paymentTypes = service.availableTypes("customer-500", "MGA", "PLATINUM");

        assertThat(bank.type()).isEqualTo("BANK");
        assertThat(card.type()).isEqualTo("CARD");
        assertThat(maucas.reference()).isNotBlank();
        assertThat(paymentTypes).contains("BANK", "CARD", "MAUCAS");
        assertThat(bank.maskedIdentifier()).endsWith("7890");
    }
}
