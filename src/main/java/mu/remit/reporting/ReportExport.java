package mu.remit.reporting;

public record ReportExport(String format, String content) {
    public ReportExport {
        if (format == null || format.isBlank()) {
            throw new IllegalArgumentException("format is required");
        }
        if (content == null) {
            throw new IllegalArgumentException("content is required");
        }
    }
}
