package mu.remit.notification;

public record NotificationTemplate(String eventType, String channel, String language, String templateText) {
    public NotificationTemplate {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("event type is required");
        }
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("channel is required");
        }
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("language is required");
        }
        if (templateText == null || templateText.isBlank()) {
            throw new IllegalArgumentException("template text is required");
        }
    }
}
