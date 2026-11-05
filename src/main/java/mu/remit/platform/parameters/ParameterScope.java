package mu.remit.platform.parameters;

import java.util.ArrayList;
import java.util.List;

public record ParameterScope(String corridor, String channel, String paymentType, String segment) {
    public static final String GLOBAL = "GLOBAL";

    public static ParameterScope global() {
        return new ParameterScope(null, null, null, null);
    }

    public List<String> precedenceOrder() {
        List<String> order = new ArrayList<>();
        if (segment != null)
            order.add("SEGMENT:" + segment);
        if (paymentType != null)
            order.add("PAYMENT_TYPE:" + paymentType);
        if (channel != null)
            order.add("CHANNEL:" + channel);
        if (corridor != null)
            order.add("CORRIDOR:" + corridor);
        order.add("GLOBAL");
        return order;
    }
}
