package mu.remit.platform.parameters;

public record ParameterValue(String key, String scope, String value) {
    public ParameterValue {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("parameter key is required");
        }
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("parameter scope is required");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("parameter value is required");
        }
    }
}
