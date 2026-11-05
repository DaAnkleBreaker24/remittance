package mu.remit.profile;

import mu.remit.platform.ParameterScope;
import mu.remit.platform.ParameterizedService;

public class ProfileService {
    private final ParameterizedService parameters;

    public ProfileService(ParameterizedService parameters) {
        this.parameters = parameters;
    }

    public ProfileSummary summary(CustomerProfile profile, long dailyUsed, long monthlyUsed) {
        if (profile == null) {
            throw new IllegalArgumentException("profile is required");
        }

        ParameterScope scope = new ParameterScope(null, null, null, profile.kycLevel());
        long dailyLimit = parseLong(parameters.getRequired("DAILY_LIMIT", scope));
        long monthlyLimit = parseLong(parameters.getRequired("MONTHLY_LIMIT", scope));

        return new ProfileSummary(
                profile.customerId(),
                profile.kycLevel(),
                dailyLimit,
                monthlyLimit,
                dailyUsed,
                monthlyUsed,
                profile.language(),
                profile.notificationChannel());
    }

    private long parseLong(String value) {
        return Long.parseLong(value.trim());
    }
}
