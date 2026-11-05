package mu.remit.platform.parameters;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ParameterRegistryTest {

    @Test
    void tracksAuditTrailAndRollbackForApprovedChanges() {
        ParameterRegistry registry = new ParameterRegistry();

        registry.register("FX_MARGIN", "GLOBAL", "3.5", "maker-a");
        ParameterVersion corridorVersion = registry.register("FX_MARGIN", "CORRIDOR:MGA", "4.0", "maker-b");

        ParameterVersion approved = registry.approve("FX_MARGIN", corridorVersion.version(), "checker-a",
                "rate increase for corridor");
        assertThat(approved.value()).isEqualTo("4.0");
        assertThat(registry.getAuditTrail("FX_MARGIN")).hasSize(2);
        assertThat(registry.getAuditTrail("FX_MARGIN").getLast().reason()).isEqualTo("rate increase for corridor");

        ParameterVersion rolledBack = registry.rollback("FX_MARGIN", corridorVersion.version(), "maker-a",
                "reverted after review");
        assertThat(rolledBack.value()).isEqualTo("4.0");
        assertThat(registry.getEffective("FX_MARGIN", new ParameterScope("MGA", null, null, null))).isEqualTo("4.0");
    }
}
