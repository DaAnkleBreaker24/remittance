package mu.remit;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {
    @Test
    void modulesHaveNoIllegalDependencies() {
        ApplicationModules.of(RemittanceApplication.class).verify();
    }
}
