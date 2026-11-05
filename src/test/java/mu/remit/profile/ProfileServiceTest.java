package mu.remit.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import mu.remit.platform.ParameterValue;
import mu.remit.platform.ParameterizedService;
import org.junit.jupiter.api.Test;

class ProfileServiceTest {

    @Test
    void displaysProfileAndLimitUsage() {
        ParameterizedService parameters = new ParameterizedService(List.of(
                new ParameterValue("DAILY_LIMIT", "SEGMENT:PLATINUM", "2000000"),
                new ParameterValue("MONTHLY_LIMIT", "SEGMENT:PLATINUM", "5000000")));

        ProfileService service = new ProfileService(parameters);
        CustomerProfile profile = new CustomerProfile("customer-400", "Alice", "PLATINUM", "en-MU", "sms");

        ProfileSummary summary = service.summary(profile, 650_000L, 1_300_000L);

        assertThat(summary.customerId()).isEqualTo("customer-400");
        assertThat(summary.kycLevel()).isEqualTo("PLATINUM");
        assertThat(summary.dailyLimit()).isEqualTo(2_000_000L);
        assertThat(summary.monthlyLimit()).isEqualTo(5_000_000L);
        assertThat(summary.dailyUsed()).isEqualTo(650_000L);
        assertThat(summary.monthlyUsed()).isEqualTo(1_300_000L);
        assertThat(summary.language()).isEqualTo("en-MU");
    }
}
