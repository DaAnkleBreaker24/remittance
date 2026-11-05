package mu.remit.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class IdentityServiceTest {

    @Test
    void registersAndAuthenticatesCustomerCredentials() {
        IdentityService service = new IdentityService();

        CustomerCredentials credentials = service.register("customer-100", "P@ssw0rd!");
        CustomerSession session = service.login("customer-100", "P@ssw0rd!");

        assertThat(credentials.passwordHash()).isNotBlank();
        assertThat(service.verifyPassword("customer-100", "P@ssw0rd!")).isTrue();
        assertThat(session.customerId()).isEqualTo("customer-100");
        assertThat(session.sessionId()).isNotBlank();
        assertThat(service.validateSession(session.sessionId())).isTrue();
    }

    @Test
    void rejectsWrongPasswordAndRevokesSession() {
        IdentityService service = new IdentityService();
        service.register("customer-200", "S3cret!");

        assertThatThrownBy(() -> service.login("customer-200", "wrong-password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid");

        CustomerSession session = service.login("customer-200", "S3cret!");
        service.revokeSession(session.sessionId());

        assertThat(service.validateSession(session.sessionId())).isFalse();
    }

    @Test
    void deviceBindingRequiresAValidSessionAndTwoFactorChallenge() {
        IdentityService service = new IdentityService();
        service.register("customer-300", "LetMeIn!2");
        CustomerSession session = service.login("customer-300", "LetMeIn!2");

        DeviceBinding binding = service.bindDevice(session.sessionId(), "device-abc", "123456");

        assertThat(binding.deviceId()).isEqualTo("device-abc");
        assertThat(binding.sessionId()).isEqualTo(session.sessionId());
        assertThat(service.isDeviceBound("customer-300", "device-abc")).isTrue();
        assertThat(service.verifyDeviceOtp("customer-300", "device-abc", "123456")).isTrue();
    }
}
