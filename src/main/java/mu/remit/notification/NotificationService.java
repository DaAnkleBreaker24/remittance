package mu.remit.notification;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class NotificationService {
    private final Map<String, List<NotificationTemplate>> templatesByEvent;

    public NotificationService(List<NotificationTemplate> templates) {
        this.templatesByEvent = templates == null ? Map.of()
                : templates.stream()
                        .collect(Collectors.groupingBy(NotificationTemplate::eventType));
    }

    public NotificationMessage send(String eventType, String channel, String language, Map<String, String> data) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("event type is required");
        }
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("channel is required");
        }
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("language is required");
        }

        List<NotificationTemplate> candidates = templatesByEvent.getOrDefault(eventType, List.of());
        NotificationTemplate template = candidates.stream()
                .filter(item -> item.channel().equalsIgnoreCase(channel))
                .filter(item -> item.language().equalsIgnoreCase(language))
                .findFirst()
                .orElseGet(() -> candidates.stream()
                        .filter(item -> item.channel().equalsIgnoreCase(channel))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("no template found for event " + eventType)));

        Map<String, String> resolved = new HashMap<>();
        if (data != null) {
            resolved.putAll(data);
        }
        String rendered = template.templateText();
        for (Map.Entry<String, String> entry : resolved.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", valueOrMask(entry.getKey(), entry.getValue()));
        }

        return new NotificationMessage(eventType, channel, language, rendered);
    }

    private String valueOrMask(String key, String value) {
        if (value == null) {
            return "";
        }
        if ("phone".equalsIgnoreCase(key) || "mobile".equalsIgnoreCase(key)) {
            return maskPhone(value);
        }
        if ("otp".equalsIgnoreCase(key) || "code".equalsIgnoreCase(key)) {
            return value;
        }
        return value;
    }

    private String maskPhone(String value) {
        String digits = value.replaceAll("\\D+", "");
        if (digits.length() <= 4) {
            return value;
        }
        return "***" + digits.substring(digits.length() - 4);
    }
}
