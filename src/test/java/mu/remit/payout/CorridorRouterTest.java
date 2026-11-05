package mu.remit.payout;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CorridorRouterTest {

    @Test
    void selectsBestMatchingCorridorAndMethod() {
        CorridorRouter router = new CorridorRouter(List.of(
                new Corridor("MGA", "MUR", "MGA",
                        List.of(
                                new PayoutMethod("MOBILE", "BANK", "MGA"),
                                new PayoutMethod("BANK", "BANK", "MGA"))),
                new Corridor("ZAR", "MUR", "ZAR",
                        List.of(
                                new PayoutMethod("BANK", "BANK", "ZAR")))));

        PayoutRoute route = router.route("MGA", "BANK", "MUR", "MGA");

        assertThat(route.corridorCode()).isEqualTo("MGA");
        assertThat(route.method().code()).isEqualTo("MOBILE");
        assertThat(route.receiveCurrency()).isEqualTo("MGA");
    }
}
