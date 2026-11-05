package mu.remit.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

    @Test
    void rendersEventTemplatesInChosenLanguageAndMasksSensitiveData() {
        NotificationService service = new NotificationService(List.of(
                new NotificationTemplate("onboarding", "sms", "en", "Hello {firstName}, your account is ready."),
                new NotificationTemplate("onboarding", "sms", "fr", "Bonjour {firstName}, votre compte est prêt."),
                new NotificationTemplate("transfer", "sms", "en",
                        "Transfer of {amount} to {beneficiary} is pending. Please keep code {otp} private.")));

        NotificationMessage welcome = service.send(
                "onboarding",
                "sms",
                "fr",
                Map.of("firstName", "Alice", "phone", "+230 12345678"));

        assertThat(welcome.eventType()).isEqualTo("onboarding");
        assertThat(welcome.channel()).isEqualTo("sms");
        assertThat(welcome.body()).isEqualTo("Bonjour Alice, votre compte est prêt.");
        assertThat(welcome.body()).doesNotContain("+230");

        NotificationMessage transfer = service.send(
                "transfer",
                "sms",
                "en",
                Map.of("amount", "5000", "beneficiary", "Jean", "otp", "123456"));

        assertThat(transfer.body()).isEqualTo("Transfer of 5000 to Jean is pending. Please keep code 123456 private.");
        assertThat(transfer.body()).contains("123456");
    }
}
