package mu.remit.notification;

public record NotificationMessage(String eventType, String channel, String language, String body) {
    public NotificationMessage {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("event type is required");
        }
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("channel is required");
        }
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("language is required");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body is required");
        }
    }
}
